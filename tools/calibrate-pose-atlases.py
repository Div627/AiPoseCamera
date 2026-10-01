from pathlib import Path
import json
src=Path('design/pose-guides/landmarks-v2-source.json')
data=json.loads(src.read_text())
for i,d in enumerate(data):
 split=490 if i//6 in [0,3] else 512
 h=split if i%6<3 else 1024-split
 scale=min(480/512,480/h);ox=(512-512*scale)/2;oy=(512-h*scale)/2
 offset=0 if i%6<3 else 512-split
 d['points']=[[round(x*scale+ox),round((y+offset)*scale+oy)] for x,y in d['points']]
 d['centerX']=round((d['centerX']*512*scale+ox)/512,5)
Path('design/pose-guides/landmarks-v2.json').write_text(json.dumps(data,ensure_ascii=False,indent=2))
p=Path('app/src/main/java/com/aipose/camera/pose/PoseAtlas.kt');s=p.read_text();a=s.index('    val entries=listOf(');b=s.index('    val centers=',a)
lines=['        Entry("%s","%s",PoseCategory.%s,PoseSupport.%s,%sf,"%s",listOf(%s))'%(e['id'],e['name'],e['category'],e['support'],e['centerX'],e['tip'],','.join('%d to %d'%tuple(v) for v in e['points'])) for e in data]
p.write_text(s[:a]+'    val entries=listOf(\n'+',\n'.join(lines)+'\n    )\n'+s[b:])
