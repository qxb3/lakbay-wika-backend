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

  // 📝 collect existing english keys (case-insensitive)
  const englishSet = new Set(
    phrases
      .map(p => p['@_english'])
      .filter(Boolean)
      .map(e => e.toLowerCase())
  );

  let newPhrases = [];

  for (let phrase of phrases) {
    const englishText = phrase['@_english'];

    if (englishText && /[\/,]/.test(englishText)) {
      let normalized = englishText.normalize('NFKC');
      normalized = normalized.replace(/[^a-zA-Z ]/g, '');

      // split by `/` or `,` with optional spaces around
      const parts = normalized.split(/[\s]*[\/,][\s]*/);

      for (let part of parts) {
        const trimmed = part.trim();
        if (!trimmed) continue;

        const lowered = trimmed.toLowerCase();

        if (englishSet.has(lowered)) {
          console.log(`Skipping duplicate phrase: "${trimmed}"`);
          continue; // skip duplicates
        }

        // add to set to avoid duplicates later
        englishSet.add(lowered);

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

  console.log(`Done! Output written to phrases_out.xml with ${newPhrases.length} new unique phrases.`);
})();

