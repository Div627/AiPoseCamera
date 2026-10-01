import AppKit
import Foundation
let sources=["883b489f-5bd1-44c9-8376-ecfd862155b4","413f1dc5-c550-43ca-b688-d6270a9916a0","1c9a5149-2b5d-439b-bec1-3652c7758de6","3be4dad7-27f2-4d3a-a1a1-0d2bf49b8566"]
let base="/Users/chenyang/.codex/generated_images/01a0ed84-f580-71d0-8333-7860d1a747bb/exec-"
for (i,id) in sources.enumerated() {
 let src=URL(fileURLWithPath:base+id+".png")
 let dest=URL(fileURLWithPath:"design/pose-guides/natural-v2-\(i).png")
 if !FileManager.default.fileExists(atPath:dest.path) {try FileManager.default.copyItem(at:src,to:dest)}
 let rep=NSBitmapImageRep(data:try Data(contentsOf:src))!;let cg=rep.cgImage!
 let packed=CGContext(data:nil,width:1536,height:1024,bitsPerComponent:8,bytesPerRow:1536*4,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 packed.interpolationQuality = .high
 // The generated A/D second-row hair begins above 512. Repack with measured row boundary.
 let split=(i==0 || i==3) ? 490 : 512
 for cell in 0..<6 {
   let top=cell<3 ? 0 : split;let h=cell<3 ? split : 1024-split
   let crop=cg.cropping(to:CGRect(x:cell%3*512,y:top,width:512,height:h))!
   let scale=min(480.0/512.0,480.0/Double(h))
   let w=512.0*scale;let height=Double(h)*scale
   packed.draw(crop,in:CGRect(x:Double(cell%3*512)+(512-w)/2,y:Double((1-cell/3)*512)+(512-height)/2,width:w,height:height))
 }
 let packedImage=packed.makeImage()!
 try NSBitmapImageRep(cgImage:packedImage).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/natural-v2-atlas-\(i).png"))
 let c=CGContext(data:nil,width:1152,height:768,bitsPerComponent:8,bytesPerRow:1152*4,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 c.interpolationQuality = .high;c.draw(packedImage,in:CGRect(x:0,y:0,width:1152,height:768))
 try NSBitmapImageRep(cgImage:c.makeImage()!).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"app/src/main/res/drawable-nodpi/pose_natural_\(i).png"))
 print("atlas",i,"source",rep.pixelsWide,rep.pixelsHigh,"alpha",rep.hasAlpha,"runtime 1152x768")
}
