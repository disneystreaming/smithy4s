import siteConfig from '@generated/docusaurus.config';

export default function prismIncludeLanguages(PrismObject) {
  const {
    themeConfig: {prism},
  } = siteConfig;
  const {additionalLanguages} = prism;

  const PrismBefore = globalThis.Prism;
  globalThis.Prism = PrismObject;

  additionalLanguages.forEach((lang) => {
    if (lang === 'smithy') {
      // Smithy has no prismjs/components/prism-smithy; registered inline below.
      return;
    }
    if (lang === 'php') {
      require('prismjs/components/prism-markup-templating.js');
    }
    // eslint-disable-next-line global-require, import/no-dynamic-require
    require(`prismjs/components/prism-${lang}`);
  });

  // Smithy IDL grammar for Prism.
  //
  // Hand-ported from the upstream TextMate grammar maintained by the Smithy
  // team — TextMate's stateful, nested begin/end rules don't translate
  // mechanically to Prism's flat token list, so this is a best-effort
  // approximation of the same token assignments.
  //
  // Source of truth (keep in sync if Smithy adds syntax):
  // https://github.com/smithy-lang/smithy-vscode/blob/main/syntaxes/smithy.tmLanguage.json
  PrismObject.languages.smithy = {
    'doc-comment': {
      pattern: /\/\/\/.*/,
      greedy: true,
      alias: 'comment',
    },
    comment: {
      pattern: /\/\/.*/,
      greedy: true,
    },
    'triple-quoted-string': {
      pattern: /"""[\s\S]*?"""/,
      greedy: true,
      alias: 'string',
    },
    string: {
      pattern: /"(?:\\.|[^"\\\n])*"/,
      greedy: true,
    },
    'control-statement': {
      // $version: "2", $operatorInputSuffix: "...", etc.
      pattern: /^\s*\$[A-Za-z_][A-Za-z0-9_]*(?=\s*:)/m,
      alias: 'keyword',
    },
    annotation: {
      // Trait applications: @http, @sxm.smithy#apiPublic, @http(method: "GET")
      pattern: /@[A-Za-z_][A-Za-z0-9_.#-]*/,
      alias: 'function',
    },
    'shape-id': {
      // Absolute shape references like com.example#Foo, sxm.smithy#apiPublic.
      // Matched before plain class-name so the namespace prefix is captured.
      pattern: /\b[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*#[A-Za-z_][A-Za-z0-9_]*(?:\$[A-Za-z_][A-Za-z0-9_]*)?/,
      alias: 'class-name',
    },
    keyword:
      /\b(?:apply|metadata|namespace|use|service|operation|resource|structure|union|list|map|set|enum|intEnum|for|with|blob|boolean|byte|short|integer|long|float|double|bigInteger|bigDecimal|string|timestamp|document)\b/,
    boolean: /\b(?:true|false|null)\b/,
    'class-name': {
      // Bare identifiers used as shape references in member targets, mixin
      // lists, etc. Smithy shape names conventionally start with an uppercase
      // letter, so this avoids over-matching member keys (which we tag as
      // `property` below).
      pattern: /\b[A-Z][A-Za-z0-9_]*\b/,
    },
    property: {
      // Member keys: `myMember: TargetShape`, `version: "v1"`. Limited to the
      // start of a line (modulo indentation) to avoid colouring every word
      // that happens to precede a colon inside trait arguments.
      pattern: /^[\t ]*[A-Za-z_][A-Za-z0-9_]*(?=\s*:)/m,
      alias: 'symbol',
    },
    number: /-?\b(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?\b/,
    operator: /:=|[=:$]/,
    punctuation: /[{}[\](),]/,
  };

  delete globalThis.Prism;
  if (typeof PrismBefore !== 'undefined') {
    globalThis.Prism = PrismObject;
  }
}
