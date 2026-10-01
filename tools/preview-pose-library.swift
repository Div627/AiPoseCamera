import AppKit
import Foundation
let entries=try JSONSerialization.jsonObject(with:Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/landmarks-v2.json"))) as! [[String:Any]]
let image=NSImage(size:NSSize(width:1800,height:1510));image.lockFocus()
NSColor(calibratedWhite:0.12,alpha:1).setFill();NSRect(x:0,y:0,width:1800,height:1510).fill()
let attrs:[NSAttributedString.Key:Any]=[.font:NSFont.systemFont(ofSize:20,weight:.medium),.foregroundColor:NSColor.white]
let title:[NSAttributedString.Key:Any]=[.font:NSFont.systemFont(ofSize:30,weight:.semibold),.foregroundColor:NSColor.white]
("姿映相机 · 24款自然姿势" as NSString).draw(at:NSPoint(x:32,y:1450),withAttributes:title)
for i in 0..<24 {
 let sheet=i/6;let cell=i%6
 let rep=NSBitmapImageRep(data:try Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/natural-v2-atlas-\(sheet).png")))!
 let cg=rep.cgImage!.cropping(to:CGRect(x:cell%3*512,y:cell/3*512,width:512,height:512))!
 let x=i%6*300;let y=1090-i/6*350
 NSImage(cgImage:cg,size:NSSize(width:512,height:512)).draw(in:NSRect(x:x,y:y,width:300,height:300))
 let label=String(format:"%02d  ",i+1)+(entries[i]["name"] as! String)
 (label as NSString).draw(at:NSPoint(x:x+35,y:y-25),withAttributes:attrs)
}
image.unlockFocus()
let rep=NSBitmapImageRep(data:image.tiffRepresentation!)!
try rep.representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/natural-24-overview-v2.png"))
