import AppKit
import Foundation
let entries=try JSONSerialization.jsonObject(with:Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/landmarks-v2.json"))) as! [[String:Any]]
for sheet in 0..<4 {
 let rep=NSBitmapImageRep(data:try Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/natural-v2-atlas-\(sheet).png")))!
 let c=CGContext(data:nil,width:1536,height:1024,bitsPerComponent:8,bytesPerRow:1536*4,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 c.setFillColor(CGColor(gray:0.25,alpha:1));c.fill(CGRect(x:0,y:0,width:1536,height:1024));c.draw(rep.cgImage!,in:CGRect(x:0,y:0,width:1536,height:1024))
 c.setStrokeColor(CGColor(red:0,green:1,blue:0.65,alpha:1));c.setLineWidth(2)
 for cell in 0..<6 {
  let ps=entries[sheet*6+cell]["points"] as! [[Int]]
  func p(_ i:Int)->CGPoint {CGPoint(x:cell%3*512+ps[i][0],y:1024-(cell/3*512+ps[i][1]))}
  for (a,b) in [(1,2),(1,3),(3,5),(2,4),(4,6),(1,7),(2,8),(7,8),(7,9),(9,11),(8,10),(10,12)] {c.move(to:p(a));c.addLine(to:p(b));c.strokePath()}
  c.setFillColor(CGColor(red:1,green:0.6,blue:0,alpha:1))
  for i in 0..<13 {let q=p(i);c.fillEllipse(in:CGRect(x:q.x-4,y:q.y-4,width:8,height:8))}
 }
 try NSBitmapImageRep(cgImage:c.makeImage()!).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/calibration-v2-\(sheet).png"))
}
