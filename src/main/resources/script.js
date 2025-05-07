const fs = require('fs/promises');
const { XMLParser, XMLBuilder } = require('fast-xml-parser');

(async () => {
  const xmlData = await fs.readFile('phrases.xml', 'utf8');

  const parser = new XMLParser({
    ignoreAttributes: false, // we NEED attributes
  });
  const builder = new XMLBuilder({
    ignoreAttributes: false,
    format: true,
    indentBy: "  ",
  });

  let json = parser.parse(xmlData);

  if (!json.phrases || !json.phrases.phrase) {
    console.error("No <phrase> entries found.");
    process.exit(1);
  }

  // force array
  let phrases = Array.isArray(json.phrases.phrase) ? json.phrases.phrase : [json.phrases.phrase];
  let newPhrases = [];

  for (let phrase of phrases) {
    const englishText = phrase['@_english'];

    if (englishText && /[\/,]/.test(englishText)) {
      let normalized = englishText.normalize('NFKC');
      // split by `/` or `,` with optional spaces around
      const parts = normalized.split(/[\s]*[\/,][\s]*/);

      for (let part of parts) {
        const trimmed = part.trim();
        if (!trimmed) continue;

        // deep clone phrase object
        const clonedPhrase = JSON.parse(JSON.stringify(phrase));
        clonedPhrase['@_english'] = trimmed;
        newPhrases.push(clonedPhrase);
      }
    }
  }

  // append clones AFTER originals
  json.phrases.phrase = phrases.concat(newPhrases);

  const newXml = builder.build(json);
  await fs.writeFile('phrases_out.xml', newXml, 'utf8');

  console.log('Done! Output written to phrases_out.xml');
})();
