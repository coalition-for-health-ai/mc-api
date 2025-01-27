import { app, HttpRequest, HttpResponseInit, InvocationContext } from "@azure/functions";
import { XMLParser } from "fast-xml-parser";
import puppeteer from "puppeteer";

const xmlParser = new XMLParser({
    preserveOrder: true,
});

function compileXMLtoHTML(xml) {
    return `
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://cdn.jsdelivr.net/npm/modern-normalize/modern-normalize.min.css"></link>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="">
        <link href="https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&amp;display=swap" rel="stylesheet">
        <style>
            :root {
                -webkit-print-color-adjust: exact;
                font-family: "Inter", sans-serif;
                font-optical-sizing: auto;
                font-style: normal;
                padding: 1in;
                word-wrap: break-word;
            }
            table {
                width: 100%;
                border: 2px solid black;
                border-collapse: collapse;
                text-align: left;
                table-layout: fixed;
            }
            tr, th, td {
                border: 2px solid black;
                padding: 4px;
            }
        </style>
    </head>
    <body>
        <!-- TODO include mc version -->
        <h1 style="text-align: center; margin-top: 0">CHAI Applied Model Card</h1>
        <table>
            <tbody>
                <tr>
                    <td colspan="3">
                        <span><b>Name:</b> TODO</span>
                        <br>
                        <span><b>Developer:</b> TODO</span>
                    </td>
                    <td colspan="3">
                        <span><b>Inquires or to report an issue:</b> TODO</span>
                    </td>
                </tr>
                <tr>
                    <td colspan="6">
                        <div style="justify-content: space-between; display: flex; flex-wrap: wrap;">
                            <span><b>Release Stage:</b> TODO</span>
                            <span><b>Release Date:</b> TODO</span>
                            <span><b>Version:</b> TODO</span>
                            <span><b>Global Availability:</b> TODO</span>
                            <span><b>Regulatory Approval:</b> TODO</span>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td colspan="3" style="position: relative">
                        <span style="position: absolute; top: 0; margin-top: 8px"><b>Summary:</b> TODO</span>
                        <br>
                        <span style="position: absolute; bottom: 0; margin-bottom: 8px"><b>Keywords:</b> TODO</span>
                    </td>
                    <td colspan="3">
                        <b>Uses and Directions:</b>
                        <ul style="margin-top: 2px">
                            <li><b>Intended use and workflow:</b> TODO</li>
                            <li><b>Primary intended users:</b> TODO</li>
                            <li><b>How to use:</b> TODO</li>
                            <li><b>Targeted patient population:</b> TODO</li>
                            <li><b>Cautioned out-of-scope settings and use cases:</b> TODO</li>
                        </ul>
                    </td>
                </tr>
                <tr style="background-color: black; color: white">
                    <td colspan="6">
                        <b>Warnings</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="6">
                        <ul style="margin-top: 4px; margin-bottom: 4px">
                            <li><b>Known risks and limitations:</b> TODO</li>
                            <li><b>Known biases or ethical considerations:</b> TODO</li>
                            <li><b>Clinical risk level:</b> TODO</li>
                        </ul>
                    </td>
                </tr>
                <tr style="background-color: black; color: white">
                    <td colspan="6">
                        <b>Trust Ingredients</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="6">
                        <b>AI System Facts:</b>
                        <ul style="margin-top: 4px; margin-bottom: 4px">
                            <li><b>Outcome(s) and output(s):</b> TODO</li>
                            <li><b>Model type:</b> TODO</li>
                            <li><b>Foundation models used in application:</b> TODO</li>
                            <li><b>Input data source:</b> TODO</li>
                            <li><b>Output/Input data type:</b> TODO</li>
                            <li><b>Development data characterization:</b> TODO</li>
                            <li><b>Bias mitigation approaches:</b> TODO</li>
                            <li><b>Ongoing Maintenance:</b> TODO</li>
                            <li><b>Security and compliance environment practices or accreditations:</b> TODO</li>
                            <li><b>Transparency, Intelligibility, and Accountability mechanisms:</b> TODO</li>
                        </ul>
                        <b>Transparency Information:</b>
                        <ul style="margin-top: 4px; margin-bottom: 4px">
                            <li><b>Funding source of the technical implementation:</b> TODO</li>
                            <li><b>3rd Party Information:</b> TODO</li>
                            <li><b>Stakeholders consulted during design of intervention (e.g. patients, providers):</b> TODO</li>
                        </ul>
                    </td>
                </tr>
                <tr style="background-color: black; color: white">
                    <td colspan="6">
                        <b>Key Metrics</b>
                    </td>
                </tr>
            </tbody>
        </table>
    </body>
</html>
`;
}

async function compileHTMLtoPDF(html) {
    const browser = await puppeteer.launch({ headless: 'shell' });
    const page = await browser.newPage();
    await page.setContent(html);
    await page.waitForFunction('document.fonts.ready');
    const pdf = await page.pdf({ format: 'Letter' });
    await browser.close();
    return pdf;
}

async function addCustomPropertiesToPDF(pdf, properties) {
    // TODO

    // const pdfDoc = await PDFDocument.load(await convertHTMLtoPDF(html));
    // let info = pdfDoc.context.lookup(pdfDoc.context.trailerInfo.Info);
    // if (!(info instanceof PDFDict)) {
    //   info = pdfDoc.context.obj({})
    //   pdfDoc.context.trailerInfo.Info = pdfDoc.context.register(info);
    // }
    // info.set(PDFName.of('chaiMcXml'), PDFString.of('TODO XML'));
    // info.set(PDFName.of('chaiMcSoftwareId'), PDFString.of('mc-api'));

    // https://github.com/Hopding/pdf-lib/issues/55#issuecomment-447880119
    return pdf;
}

export async function XmlToPdf(request: HttpRequest, context: InvocationContext): Promise<HttpResponseInit> {
    if (request.method === 'OPTIONS') {
        return {
            status: 204,
            headers: {
                "Allow": ["OPTIONS", "POST"],
                "Accept-Post": ["text/xml", "application/xml"],
            }
        };
    }

    if (request.headers.get('content-type') !== 'text/xml' && request.headers.get('content-type') !== 'application/xml') {
        return {
            status: 415,
            headers: {
                "Accept-Post": ["text/xml", "application/xml"],
            }
        };
    }

    const body = await request.text();
    context.log(`ENTRY "${body}"`);

    const xml = xmlParser.parse(body);
    const html = compileXMLtoHTML(xml);
    const pdf = await compileHTMLtoPDF(html);
    const pdfWithProperties = await addCustomPropertiesToPDF(pdf, {
        chaiMcXml: xml,
        chaiMcSoftwareId: 'mc-api',
    });

    context.log("RETURN");
    return { body: pdfWithProperties, headers: { 'Content-Type': 'application/pdf' } };
};

app.http('XmlToPdf', {
    methods: ['OPTIONS', 'POST'],
    authLevel: 'anonymous',
    handler: XmlToPdf
});
