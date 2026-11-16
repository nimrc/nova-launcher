import AppKit
// title.swift in out.jpg "中文名" "ENGLISH SUBTITLE" [obi text] [obi hex color]
// Draws a bottom title block onto a 600x900 cover; with an obi text, adds a book-obi style band below it.
let a = CommandLine.arguments
let W: CGFloat = 600, H: CGFloat = 900
let obi = a.count > 5 ? a[5] : nil
let obiColor: NSColor = {
    guard a.count > 6, let v = Int(a[6].trimmingCharacters(in: CharacterSet(charactersIn: "#")), radix: 16) else { return .systemRed }
    return NSColor(red: CGFloat(v >> 16 & 255) / 255, green: CGFloat(v >> 8 & 255) / 255, blue: CGFloat(v & 255) / 255, alpha: 1)
}()

let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(W), pixelsHigh: Int(H), bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
let ctx = NSGraphicsContext(bitmapImageRep: rep)!
ctx.imageInterpolation = .high
NSGraphicsContext.current = ctx
NSImage(contentsOfFile: a[1])!.draw(in: NSRect(x: 0, y: 0, width: W, height: H))
NSGradient(colors: [NSColor.black.withAlphaComponent(0.85), NSColor.black.withAlphaComponent(0)])!
    .draw(in: NSRect(x: 0, y: 0, width: W, height: obi == nil ? 340 : 420), angle: 90)

let shadow = NSShadow()
shadow.shadowColor = NSColor.black.withAlphaComponent(0.8)
shadow.shadowBlurRadius = 10
shadow.shadowOffset = NSSize(width: 0, height: -2)
let center = NSMutableParagraphStyle()
center.alignment = .center

func text(_ s: String, _ font: NSFont, _ color: NSColor, kern: CGFloat, shadow: NSShadow? = shadow) -> NSAttributedString {
    var attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color, .kern: kern, .paragraphStyle: center]
    if let shadow { attrs[.shadow] = shadow }
    return NSAttributedString(string: s, attributes: attrs)
}

var base: CGFloat = 70
if let obi {
    // Band with thin light rules, like a printed obi strip.
    let band = NSRect(x: 0, y: 40, width: W, height: 64)
    obiColor.setFill()
    band.fill()
    NSColor.white.withAlphaComponent(0.55).setFill()
    NSRect(x: 0, y: band.maxY - 6, width: W, height: 1.5).fill()
    NSRect(x: 0, y: band.minY + 4.5, width: W, height: 1.5).fill()
    text(obi, NSFont(name: "PingFangSC-Semibold", size: 30)!, .white, kern: 8, shadow: nil)
        .draw(in: NSRect(x: 0, y: band.minY + 11, width: W, height: 44))
    base = 128
}
text(a[3], NSFont(name: "PingFangSC-Semibold", size: 76)!, .white, kern: 6)
    .draw(in: NSRect(x: 0, y: base + 42, width: W, height: 110))
text(a[4], .systemFont(ofSize: 22, weight: .bold), NSColor(white: 1, alpha: 0.85), kern: 5)
    .draw(in: NSRect(x: 0, y: base, width: W, height: 34))
NSGraphicsContext.restoreGraphicsState()
try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.85])!.write(to: URL(fileURLWithPath: a[2]))
