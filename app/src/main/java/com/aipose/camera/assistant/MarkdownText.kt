package com.aipose.camera.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.*
import org.commonmark.parser.Parser

private data class MarkdownBlock(val text: AnnotatedString,val heading:Int=0,val code:Boolean=false,val table:Boolean=false)
private val markdownCache=object:android.util.LruCache<String,List<MarkdownBlock>>(200_000) {
    override fun sizeOf(key:String,value:List<MarkdownBlock>)=key.length.coerceAtLeast(1)
}
private val markdownParser=Parser.builder().extensions(listOf<Extension>(TablesExtension.create())).build()

/** Parse each immutable message once, off the UI thread. Unclosed fences are valid CommonMark. */
@Composable
fun MarkdownText(text: String) {
    val blocks by produceState<List<MarkdownBlock>?>(null,text) {
        value=withContext(Dispatchers.Default) {parseMarkdown(text)}
    }
    val links=LocalUriHandler.current
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(blocks==null) androidx.compose.material3.Text(text.take(500),style=MaterialTheme.typography.bodyMedium)
        blocks?.forEach {block ->
            val scroll=if(block.code || block.table) Modifier.horizontalScroll(rememberScrollState()) else Modifier
            ClickableText(block.text,Modifier.fillMaxWidth().then(scroll).then(
                if(block.code) Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp) else Modifier),
                style=when {block.heading==1 -> MaterialTheme.typography.titleMedium;block.heading>1 -> MaterialTheme.typography.titleSmall;else -> MaterialTheme.typography.bodyLarge}.copy(
                    color=MaterialTheme.colorScheme.onSurface,fontFamily=if(block.code || block.table) FontFamily.Monospace else FontFamily.Default),
                onClick={offset -> block.text.getStringAnnotations("url",offset,offset).firstOrNull()?.item?.let {uri ->
                    if(uri.startsWith("https://") || uri.startsWith("http://")) runCatching {links.openUri(uri)}
                }})
        }
    }
}
private fun parseMarkdown(text: String): List<MarkdownBlock> {
    synchronized(markdownCache) {markdownCache.get(text)?.let {return it}}
    val document=markdownParser.parse(text)
    val result=mutableListOf<MarkdownBlock>()
    var block=document.firstChild
    while(block!=null) {
        val current=block
        val output=AnnotatedString.Builder()
        fun append(node: Node) {
            when(node) {
                is Text -> output.append(node.literal)
                is Code -> {output.pushStyle(SpanStyle(fontFamily=FontFamily.Monospace));output.append(node.literal);output.pop()}
                is SoftLineBreak, is HardLineBreak -> output.append("\n")
                is FencedCodeBlock -> output.append(node.literal.trimEnd())
                is IndentedCodeBlock -> output.append(node.literal.trimEnd())
                is StrongEmphasis -> {output.pushStyle(SpanStyle(fontWeight=FontWeight.Bold));children(node,::append);output.pop()}
                is Emphasis -> {output.pushStyle(SpanStyle(fontStyle=FontStyle.Italic));children(node,::append);output.pop()}
                is Link -> {output.pushStringAnnotation("url",node.destination);output.pushStyle(SpanStyle(color=Color(0xff3568a8)));children(node,::append);output.pop();output.pop()}
                is Image -> {output.append("[图片] ");children(node,::append)}
                is ListItem -> {output.append("• ");children(node,::append);output.append("\n")}
                is Paragraph -> children(node,::append)
                else -> {
                    children(node,::append)
                    if(node is TableCell) output.append("  │  ")
                    if(node is TableRow) output.append("\n")
                }
            }
        }
        append(current)
        result+=MarkdownBlock(output.toAnnotatedString(),(current as? Heading)?.level ?: 0,
            current is FencedCodeBlock || current is IndentedCodeBlock,current is TableBlock)
        block=block.next
    }
    synchronized(markdownCache) {markdownCache.put(text,result)}
    return result
}
private fun children(node:Node,visit:(Node)->Unit) {
    var child=node.firstChild
    while(child!=null) {visit(child);child=child.next}
}
