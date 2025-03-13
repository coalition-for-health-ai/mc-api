# mc-api

## Usage

If using `curl`, be sure to use the `--data-binary` flag (and not the `-d` shorthand) to avoid stripping newlines from the XML.

### `ValidateXml`

```sh
curl -X POST --data-binary @input.xml -H "Content-Type: text/xml" https://api.mc.chai.org/api/ValidateXml -D -
```

### `XmlToPdf`

```sh
curl -X POST --data-binary @input.xml -H "Content-Type: text/xml" https://api.mc.chai.org/api/XmlToPdf -o output.pdf
```

## Documentation

The endpoints offered by this API are documented via an OpenAPI schema: [`./openapi.yaml`](./openapi.yaml)
