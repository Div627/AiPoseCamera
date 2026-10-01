import Foundation
import CoreImage
import AppKit
import Vision

let url = CommandLine.arguments[1]
let output = CommandLine.arguments[2]
let filter = CIFilter(name: "CIQRCodeGenerator")!
filter.setValue(url.data(using: .utf8)!, forKey: "inputMessage")
filter.setValue("M", forKey: "inputCorrectionLevel")
let image = filter.outputImage!.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
let cg = CIContext().createCGImage(image, from: image.extent)!
let margin = 40
let w = cg.width + margin * 2
let ctx = CGContext(data: nil, width: w, height: w, bitsPerComponent: 8, bytesPerRow: w*4,
                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
ctx.setFillColor(CGColor(gray: 1, alpha: 1)); ctx.fill(CGRect(x:0,y:0,width:w,height:w))
ctx.interpolationQuality = .none
ctx.draw(cg, in: CGRect(x:margin,y:margin,width:cg.width,height:cg.height))
let final = ctx.makeImage()!
let png = NSBitmapImageRep(cgImage: final).representation(using:.png, properties:[:])!
try png.write(to: URL(fileURLWithPath:output))
let request = VNDetectBarcodesRequest()
request.symbologies = [.qr]
try VNImageRequestHandler(cgImage:final).perform([request])
guard request.results?.first?.payloadStringValue == url else { fatalError("QR decode verification failed") }
print("QR verified: \(url) -> \(output)")
