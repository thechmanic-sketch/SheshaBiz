/**
 * Client-side PDF export for the quote/invoice/receipt previews.
 *
 * jsPDF by itself only draws text/shapes/images programmatically — it has
 * no notion of HTML/CSS layout. Its `.html()` plugin closes that gap: it
 * rasterizes a live DOM node through html2canvas (an optional dependency
 * of jspdf that's already pulled in by `npm install`, so no new package
 * was added just for this) and drops the resulting image into the PDF.
 * That means the exported file matches whatever `DocumentPreview` is
 * already rendering on screen — there's no second copy of the document
 * layout to keep in sync with it.
 *
 * Everything here runs in the browser with no server involved, so it
 * works from the static export and inside the offline Tauri desktop build.
 */
export async function downloadDocumentAsPdf(elementId: string, filename: string): Promise<void> {
  if (typeof window === "undefined") return;

  const element = document.getElementById(elementId);
  if (!element) return;

  const { default: jsPDF } = await import("jspdf");
  const doc = new jsPDF({ unit: "pt", format: "a4" });

  const margin = 24;
  const targetWidth = doc.internal.pageSize.getWidth() - margin * 2;
  const sourceWidth = element.scrollWidth || element.offsetWidth || 700;
  const name = filename.toLowerCase().endsWith(".pdf") ? filename : `${filename}.pdf`;

  await new Promise<void>((resolve, reject) => {
    doc
      .html(element, {
        x: margin,
        y: margin,
        width: targetWidth,
        windowWidth: sourceWidth,
        // Avoids slicing a line of text across a page break.
        autoPaging: "text",
        html2canvas: { useCORS: true, backgroundColor: "#ffffff" },
        callback: (pdfDoc) => {
          pdfDoc.save(name);
          resolve();
        },
      })
      .catch(reject);
  });
}
