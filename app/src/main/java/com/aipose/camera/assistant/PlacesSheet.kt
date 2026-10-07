package com.aipose.camera.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.*

data class MapCenter(val name:String,val lat:Double,val lon:Double)
data class PhotoPlace(val name:String,val category:String,val lat:Double,val lon:Double,val distanceKm:Double)
object PlaceLookup {
    private val client=OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
    private fun json(url:String): JSONObject {
        val request=Request.Builder().url(url).header("User-Agent","YingkeCamera/0.1 (https://github.com/Div627/AiPoseCamera)").build()
        return client.newCall(request).execute().use {response ->
            check(response.isSuccessful)
            val body=response.body ?: error("No response")
            body.byteStream().use {input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;require(out.size()+n<=2*1024*1024);out.write(buffer,0,n)}
                JSONObject(out.toString("UTF-8"))
            }
        }
    }
    suspend fun city(name:String): List<MapCenter> = withContext(Dispatchers.IO) {
        val query=when(name.trim()) {"冰岛"->"Iceland";"雷克雅未克","雷克雅维克"->"Reykjavik Iceland";else->name.trim()}
        require(query.isNotBlank() && query.length<=120)
        val url="https://photon.komoot.io/api/".toHttpUrl().newBuilder().addQueryParameter("q",query).addQueryParameter("limit","5").build()
        val features=json(url.toString()).getJSONArray("features")
        (0 until features.length()).map {index ->
            val item=features.getJSONObject(index);val properties=item.getJSONObject("properties")
            val coordinates=item.getJSONObject("geometry").getJSONArray("coordinates")
            val label=listOf(properties.optString("name"),properties.optString("city"),properties.optString("state"),properties.optString("country")).filter {it.isNotBlank()}.distinct().joinToString(" · ")
            MapCenter(label,coordinates.getDouble(1),coordinates.getDouble(0))
        }.distinctBy {it.name}
    }
    suspend fun nearby(lat:Double,lon:Double): List<PhotoPlace> = withContext(Dispatchers.IO) {
        require(lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0)
        val url="https://photon.komoot.io/reverse".toHttpUrl().newBuilder()
            .addQueryParameter("lat",lat.toString()).addQueryParameter("lon",lon.toString())
            .addQueryParameter("radius","30").addQueryParameter("limit","12")
            .addQueryParameter("osm_tag","tourism:viewpoint").addQueryParameter("osm_tag","waterway:waterfall")
            .addQueryParameter("osm_tag","natural:beach").build()
        val features=json(url.toString()).getJSONArray("features")
        (0 until features.length()).mapNotNull {index ->
            val item=features.getJSONObject(index);val tags=item.optJSONObject("properties") ?: return@mapNotNull null
            val coordinates=item.getJSONObject("geometry").getJSONArray("coordinates")
            val x=coordinates.optDouble(1,Double.NaN);val y=coordinates.optDouble(0,Double.NaN)
            if(!x.isFinite() || !y.isFinite()) return@mapNotNull null
            val type=when(tags.optString("osm_value")) {"viewpoint"->"观景点";"waterfall"->"瀑布";"beach"->"海滩";else->return@mapNotNull null}
            val name=tags.optString("name").ifBlank {"未命名$type"}.take(100)
            PhotoPlace(name,type,x,y,distance(lat,lon,x,y))
        }.filter {it.distanceKm<=30}.distinctBy {Triple(it.name,it.lat,it.lon)}.sortedBy {it.distanceKm}.take(6)
    }

    fun distance(lat:Double,lon:Double,x:Double,y:Double):Double {
        val a=sin(Math.toRadians(x-lat)/2).pow(2)+cos(Math.toRadians(lat))*cos(Math.toRadians(x))*sin(Math.toRadians(y-lon)/2).pow(2)
        return 6371*2*asin(sqrt(a.coerceIn(0.0,1.0)))
    }
}
@android.annotation.SuppressLint("MissingPermission")
private suspend fun position(context:Context): Location? {
    if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return null
    val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers=manager.getProviders(true).filter {fine && it==LocationManager.GPS_PROVIDER || it==LocationManager.NETWORK_PROVIDER}
    val cached=providers.mapNotNull {runCatching {manager.getLastKnownLocation(it)}.getOrNull()}.filter {System.currentTimeMillis()-it.time<15*60*1000}.minByOrNull {it.accuracy}
    if(cached!=null) return cached
    if(Build.VERSION.SDK_INT<30) return null
    val provider=providers.firstOrNull() ?: return null
    return withTimeoutOrNull(12000) {suspendCancellableCoroutine {continuation ->
        val cancel=CancellationSignal();continuation.invokeOnCancellation {cancel.cancel()}
        try {manager.getCurrentLocation(provider,cancel,ContextCompat.getMainExecutor(context)) {location -> if(continuation.isActive) continuation.resume(location)}}
        catch(_:Exception) {if(continuation.isActive) continuation.resume(null)}
    }}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesSheet(onClose:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var city by remember {mutableStateOf("")};var places by remember {mutableStateOf<List<PhotoPlace>>(emptyList())}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf<String?>(null)}
    var centers by remember {mutableStateOf<List<MapCenter>>(emptyList())}
    var center by remember {mutableStateOf<String?>(null)}
    fun search(useLocation:Boolean,selected:MapCenter?=null) {
        if(busy) return
        busy=true;message=null;places=emptyList();center=null
        scope.launch {
            try {
                if(!useLocation && selected==null) {
                    centers=PlaceLookup.city(city)
                    if(centers.isEmpty()) message="没有找到城市，请加上国家或更具体的地区。"
                } else {
                    centers=emptyList()
                    val coords=if(useLocation) position(context)?.let {it.latitude to it.longitude} ?: error("No location") else selected!!.lat to selected.lon
                    center=if(useLocation) "当前位置附近" else "${selected!!.name} 附近"
                    places=PlaceLookup.nearby(coords.first,coords.second)
                    if(places.isEmpty()) message="地图中没有找到附近拍摄点，可换一个更具体的城市或地区。"
                }
            } catch(_:Exception) {message="查询未完成。请检查网络与定位，或输入具体城市后重试。"}
            finally {busy=false}
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {result ->
        if(result.values.any {it}) search(true) else message="没有定位授权，也可以输入城市查询。"
    }
    ModalBottomSheet(onDismissRequest=onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("去哪里拍",style=MaterialTheme.typography.titleLarge)
            Text("从地图寻找观景点、瀑布与海滩。查询会把城市或位置发送给地图服务；不上传照片或视频。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(city,{city=it},Modifier.fillMaxWidth(),placeholder={Text("城市和国家，例如 Reykjavík Iceland")},singleLine=true)
            Row {TextButton(enabled=!busy && city.isNotBlank(),onClick={search(false)}){Text("按城市查询")}
                TextButton(enabled=!busy,onClick={if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) search(true) else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))}){Text("使用当前位置")}}
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            center?.let {Text(it,style=MaterialTheme.typography.labelMedium)}
            message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            LazyColumn(Modifier.heightIn(max=320.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(centers,key={it.name}) {candidate -> TextButton(enabled=!busy,onClick={search(false,candidate)}){Text("查询这里附近：${candidate.name}")} }
                items(places,key={"${it.lat},${it.lon}"}) {place -> Column {
                    Text(place.name,style=MaterialTheme.typography.titleMedium)
                    Text("${place.category} · 直线约 ${"%.1f".format(place.distanceKm)} km",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={
                        val query="${place.lat},${place.lon} (${place.name})"
                        val geo=Uri.parse("geo:${place.lat},${place.lon}?q=${Uri.encode(query)}")
                        val intent=Intent(Intent.ACTION_VIEW,geo)
                        runCatching {context.startActivity(intent)}.onFailure {runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/?mlat=${place.lat}&mlon=${place.lon}#map=14/${place.lat}/${place.lon}")))}}
                    }){Text("在地图中打开")}
                }}
            }
            Text("来源：OpenStreetMap / Photon。按直线距离排序；路况、开放状态、天气和拍摄时间尚未核验。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}
