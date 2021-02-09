import { terser } from 'rollup-plugin-terser';
import minifyHTML from 'rollup-plugin-minify-html-literals';
import resolve from '@rollup/plugin-node-resolve';
import path from 'path';

// Replace imports by an export of existing globals in Web UI
// https://github.com/nuxeo/nuxeo-web-ui/blob/master/index.js#L7
const GLOBALS = {
  '@polymer/polymer/lib/legacy/class.js': 'const { mixinBehaviors } = Polymer; export { mixinBehaviors };',
  '@polymer/polymer/polymer-element.js': 'const { PolymerElement } = window; export { PolymerElement };',
  '@polymer/polymer/lib/utils/html-tag.js': 'const { html } = Polymer; export { html };',
  '@nuxeo/nuxeo-ui-elements/nuxeo-i18n-behavior.js': 'const { I18nBehavior } = Nuxeo; export { I18nBehavior };',
  '@nuxeo/moment/moment.js': 'const { moment } = window; export default moment;',
};

// Ignore these imports since they should just be all about custom element definitions which are done already by Web UI
const IGNORES = [/^@(nuxeo|polymer)\//];

// Keep these imports
const KEEP = ['@nuxeo/nuxeo-ui-elements/import-href.js'];

const TARGET = 'target/classes/web/nuxeo.war/ui';

export default {
  input: './src/index.js',
  output: {
    file: `${TARGET}/nuxeo-statistics.bundle.js`,
    format: 'es',
  },
  plugins: [
    resolve(),
    {
      transform(code, id) {
        // HTML imports
        if (path.extname(id) === '.html') {
          return `export default ${JSON.stringify(code)}`;
        }

        const dep = path.relative('./node_modules', id);

        // Rewrite imports
        if (GLOBALS[dep]) {
          return GLOBALS[dep];
        }

        // Ignore bundled imports
        if (!KEEP.includes(dep) && IGNORES.some((r) => r.test(dep))) {
          return 'export default undefined;';
        }

        return code;
      },
    },
    ...(process.env.NODE_ENV === 'production' ? [minifyHTML(), terser()] : []),
  ],
};
