import AppKit
import Vision
import CoreImage
let entries=try JSONSerialization.jsonObject(with:Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/editorial-v4/manifest.json"))) as! [[String:Any]]
let ci=CIContext()
for atlas in 0..<4 {
 let path="design/pose-guides/natural-v3-atlas-\(atlas).png"
 let source=NSBitmapImageRep(data:try Data(contentsOf:URL(fileURLWithPath:path)))!.cgImage!
 func context()->CGContext {CGContext(data:nil,width:1536,height:1024,bitsPerComponent:8,bytesPerRow:6144,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!}
 let photos=context();photos.draw(source,in:CGRect(x:0,y:0,width:1536,height:1024))
 let contours=context();let old=NSImage(contentsOfFile:"design/pose-guides/contour-v3-\(atlas).png")!.cgImage(forProposedRect:nil,context:nil,hints:nil)!
 contours.draw(old,in:CGRect(x:0,y:0,width:1536,height:1024))
 for e in entries where (e["index"] as! Int)/6==atlas {
  let index=e["index"] as! Int;let cell=index%6
  let original=NSBitmapImageRep(data:try Data(contentsOf:URL(fileURLWithPath:"design/pose-guides/editorial-v4/source-\(index).png")))!.cgImage!
  let request=VNGeneratePersonSegmentationRequest();request.qualityLevel = .accurate;request.outputPixelFormat=kCVPixelFormatType_OneComponent8
  try VNImageRequestHandler(cgImage:original).perform([request]);let raw=CIImage(cvPixelBuffer:request.results!.first!.pixelBuffer)
  let mask=raw.transformed(by:CGAffineTransform(scaleX:1024/raw.extent.width,y:1536/raw.extent.height))
  let image=CIImage(cgImage:original);let clear=CIImage(color:.clear).cropped(to:image.extent)
  let cutout=image.applyingFilter("CIBlendWithMask",parameters:[kCIInputBackgroundImageKey:clear,kCIInputMaskImageKey:mask])
  let cut=ci.createCGImage(cutout,from:image.extent)!
  let dest=CGRect(x:cell%3*512,y:(1-cell/3)*512,width:512,height:512)
  photos.clear(dest);contours.clear(dest)
  let scale=472.0/1536;let ox=256-1024*scale/2
  photos.draw(cut,in:CGRect(x:Double(cell%3*512)+ox,y:Double((1-cell/3)*512)+20,width:1024*scale,height:472))
  // Portrait source thumbnails keep their photographic setting, only the guide cutout removes it.
  let thumb=CGContext(data:nil,width:256,height:384,bitsPerComponent:8,bytesPerRow:1024,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.noneSkipLast.rawValue)!
  thumb.draw(original,in:CGRect(x:0,y:0,width:256,height:384))
  try NSBitmapImageRep(cgImage:thumb.makeImage()!).representation(using:.jpeg,properties:[.compressionFactor:0.88])!.write(to:URL(fileURLWithPath:"app/src/main/res/drawable-nodpi/pose_editorial_\(index).jpg"))
  let maskCg=ci.createCGImage(mask,from:image.extent)!
  let contourReq=VNDetectContoursRequest();contourReq.detectsDarkOnLight=false;contourReq.maximumImageDimension=1024
  try VNImageRequestHandler(cgImage:maskCg).perform([contourReq])
  contours.saveGState();contours.translateBy(x:CGFloat(cell%3*512)+ox,y:CGFloat((1-cell/3)*512)+20);contours.scaleBy(x:1024*scale,y:1536*scale)
  contours.setStrokeColor(CGColor(gray:1,alpha:1));contours.setLineWidth(1.6/472);contours.setLineJoin(.round);contours.setLineCap(.round)
  func trace(_ c:VNContour) {if c.normalizedPath.boundingBox.width*c.normalizedPath.boundingBox.height>0.001 {let simple=(try? c.polygonApproximation(epsilon:0.0012)) ?? c;contours.addPath(simple.normalizedPath);contours.strokePath();for child in c.childContours {trace(child)}}}
  for c in contourReq.results!.first!.topLevelContours {trace(c)}
  contours.restoreGState()
  contours.saveGState();contours.translateBy(x:CGFloat(cell%3*512)+ox,y:CGFloat((1-cell/3)*512)+20+472);contours.scaleBy(x:scale,y:-scale)
  contours.setStrokeColor(CGColor(gray:1,alpha:0.85));contours.setLineWidth(1.35/scale);contours.setLineJoin(.round);contours.setLineCap(.round)
  for line in e["internal"] as! [[[Int]]] {for (i,p) in line.enumerated(){if i==0 {contours.move(to:CGPoint(x:p[0],y:p[1]))}else{contours.addLine(to:CGPoint(x:p[0],y:p[1]))}};contours.strokePath()}
  contours.restoreGState()
 }
 for (name,cg) in [("natural",photos.makeImage()!),("outline",contours.makeImage()!)] {
  try NSBitmapImageRep(cgImage:cg).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"design/pose-guides/editorial-v4/\(name)-atlas-\(atlas).png"))
  let small=CGContext(data:nil,width:1152,height:768,bitsPerComponent:8,bytesPerRow:4608,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue)!
  small.interpolationQuality = .high;small.draw(cg,in:CGRect(x:0,y:0,width:1152,height:768))
  try NSBitmapImageRep(cgImage:small.makeImage()!).representation(using:.png,properties:[:])!.write(to:URL(fileURLWithPath:"app/src/main/res/drawable-nodpi/pose_\(name)_\(atlas).png"))
 }
 print("v4 atlas",atlas)
}
