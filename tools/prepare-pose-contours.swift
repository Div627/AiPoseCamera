import AppKit
import Vision
import CoreImage
let ci=CIContext()
for atlas in 0..<4 {
 let path=atlas==0 ? "design/pose-guides/natural-v3-0.png" : "design/pose-guides/natural-v2-atlas-\(atlas).png"
 let source=NSBitmapImageRep(data:try Data(contentsOf:URL(fileURLWithPath:path)))!.cgImage!
 let packed=CGContext(data:nil,width:1536,height:1024,bitsPerComponent:8,bytesPerRow:6144,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 // New atlas has full 512px cells. Add 20px safety padding, matching the calibration transform.
 if atlas==0 {
 for cell in 0..<6 {
  let crop=source.cropping(to:CGRect(x:cell%3*512,y:cell/3*512,width:512,height:512))!
  packed.draw(crop,in:CGRect(x:cell%3*512+20,y:(1-cell/3)*512+20,width:472,height:472))
 }
 } else {packed.draw(source,in:CGRect(x:0,y:0,width:1536,height:1024))}
 let photo=packed.makeImage()!
 if atlas==0 {try NSBitmapImageRep(cgImage:photo).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/natural-v3-atlas-0.png"))}
 let output=CGContext(data:nil,width:1536,height:1024,bitsPerComponent:8,bytesPerRow:6144,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
 for cell in 0..<6 {
  let crop=photo.cropping(to:CGRect(x:cell%3*512,y:cell/3*512,width:512,height:512))!
  let request=VNGeneratePersonSegmentationRequest();request.qualityLevel = .accurate;request.outputPixelFormat=kCVPixelFormatType_OneComponent8
  try VNImageRequestHandler(cgImage:crop).perform([request])
  guard let mask=request.results?.first?.pixelBuffer else {fatalError("No person mask")}
  let rawMask=CIImage(cvPixelBuffer:mask);let scaled=rawMask.transformed(by:CGAffineTransform(scaleX:512/rawMask.extent.width,y:512/rawMask.extent.height))
  let cgMask=ci.createCGImage(scaled,from:CGRect(x:0,y:0,width:512,height:512))!
  let gray=CGContext(data:nil,width:512,height:512,bitsPerComponent:8,bytesPerRow:512,space:CGColorSpaceCreateDeviceGray(),bitmapInfo:0)!
  gray.draw(cgMask,in:CGRect(x:0,y:0,width:512,height:512));let bytes=gray.data!.assumingMemoryBound(to:UInt8.self)
  // Threshold segmentation; no photographic texture edges, no ground shadows. Vision excludes furniture.
  let binary=CGContext(data:nil,width:512,height:512,bitsPerComponent:8,bytesPerRow:512,space:CGColorSpaceCreateDeviceGray(),bitmapInfo:0)!
  let b=binary.data!.assumingMemoryBound(to:UInt8.self)
  for i in 0..<512*512 {b[i]=bytes[i]>128 ? 255 : 0}
  let contours=VNDetectContoursRequest();contours.detectsDarkOnLight=false;contours.maximumImageDimension=512
  try VNImageRequestHandler(cgImage:binary.makeImage()!).perform([contours])
  guard let obs=contours.results?.first else {fatalError("No contour")}
  output.saveGState();output.translateBy(x:CGFloat(cell%3*512),y:CGFloat((1-cell/3)*512));output.scaleBy(x:512,y:512)
  output.setStrokeColor(CGColor(gray:1,alpha:1));output.setLineWidth(1.7/512);output.setLineJoin(.round);output.setLineCap(.round)
  func draw(_ c:VNContour) {
    let box=c.normalizedPath.boundingBox
    if box.width*box.height>0.001 {
      let simple=(try? c.polygonApproximation(epsilon:0.0018)) ?? c
      output.addPath(simple.normalizedPath);output.strokePath()
      for child in c.childContours {draw(child)}
    }
  }
  for c in obs.topLevelContours {draw(c)}
  output.restoreGState()
 }
 let line=output.makeImage()!
 try NSBitmapImageRep(cgImage:line).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/contour-v3-\(atlas).png"))
 for (name,cg) in [("pose_outline_\(atlas)",line),("pose_natural_\(atlas)",photo)] {
  let small=CGContext(data:nil,width:1152,height:768,bitsPerComponent:8,bytesPerRow:4608,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
  small.interpolationQuality = .high;small.draw(cg,in:CGRect(x:0,y:0,width:1152,height:768))
  try NSBitmapImageRep(cgImage:small.makeImage()!).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"app/src/main/res/drawable-nodpi/\(name).png"))
 }
 print("Prepared photos and human-only contours",atlas)
}
