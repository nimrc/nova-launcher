import AppKit
// crop.swift in.png out.jpg [W H] : center-crop to 2:3 then scale to 600x900 JPEG
let a = CommandLine.arguments
let img = NSImage(contentsOfFile: a[1])!
var rect = NSRect(origin: .zero, size: img.size)
let rep = img.representations[0]
let w = CGFloat(rep.pixelsWide), h = CGFloat(rep.pixelsHigh)
let target: CGFloat = 2.0/3.0
var cw = w, ch = h
if w/h > target { cw = h*target } else { ch = w/target }
let cg = img.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
let cropped = cg.cropping(to: CGRect(x: (w-cw)/2, y: a.count > 3 && a[3] == "top" ? 0 : (h-ch)/2, width: cw, height: ch))!
let out = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 600, pixelsHigh: 900, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
let ctx = NSGraphicsContext(bitmapImageRep: out)!
ctx.imageInterpolation = .high
NSGraphicsContext.current = ctx
ctx.cgContext.draw(cropped, in: CGRect(x: 0, y: 0, width: 600, height: 900))
NSGraphicsContext.restoreGraphicsState()
try! out.representation(using: .jpeg, properties: [.compressionFactor: 0.85])!.write(to: URL(fileURLWithPath: a[2]))
