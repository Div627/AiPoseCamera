import AppKit
import Foundation
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let source = NSImage(contentsOf: root.appendingPathComponent("design/ziying-camera-icon-v1.png"))!
var rect = CGRect(origin:.zero,size:source.size)
let cg = source.cgImage(forProposedRect:&rect,context:nil,hints:nil)!
func render(_ size:Int,_ fraction:CGFloat,_ path:String,_ transparent:Bool=false,_ round:Bool=false) throws {
 let c=CGContext(data:nil,width:size,height:size,bitsPerComponent:8,bytesPerRow:size*4,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 if round {c.addEllipse(in:CGRect(x:0,y:0,width:size,height:size));c.clip()}
 if !transparent { c.setFillColor(CGColor(red:1,green:0.80,blue:0.89,alpha:1));c.fill(CGRect(x:0,y:0,width:size,height:size)) }
 let side=CGFloat(size)*fraction
 c.interpolationQuality = .high
 c.draw(cg,in:CGRect(x:(CGFloat(size)-side)/2,y:(CGFloat(size)-side)/2,width:side,height:side))
 let url=root.appendingPathComponent(path)
 try FileManager.default.createDirectory(at:url.deletingLastPathComponent(),withIntermediateDirectories:true)
 try NSBitmapImageRep(cgImage:c.makeImage()!).representation(using:.png,properties:[:])!.write(to:url)
}
for (density,size) in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)] {try render(size,0.94,"app/src/main/res/mipmap-\(density)/ic_launcher.png");try render(size,0.80,"app/src/main/res/mipmap-\(density)/ic_launcher_round.png",false,true)}
try render(432,0.61,"app/src/main/res/drawable-nodpi/ic_launcher_foreground.png",true)
try render(512,0.94,"design/launcher-preview.png")
