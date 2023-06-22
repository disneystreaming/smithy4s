// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'smithy4s',
  tagline: 'Smithy tooling for Scala',
  url: 'https://disneystreaming.github.io',
  baseUrl: '/smithy4s/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon-32x32.png',
  organizationName: 'disneystreaming', // Usually your GitHub org/user name.
  projectName: 'smithy4s', // Usually your repo name.
  plugins: [require.resolve('docusaurus-lunr-search')],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: function ({ docPath }) {
            return `https://github.com/disneystreaming/smithy4s/edit/main/modules/docs/src/${docPath}`
          },
          path: '../docs/target/jvm-2.13/mdoc'
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
        blog: false,
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'smithy4s',
        logo: {
          alt: 'Disney Streaming Logo',
          src: 'img/logo.svg',
          srcDark: 'img/logoDark.svg',
        },
        items: [
          {
            type: 'doc',
            docId: 'overview/intro',
            position: 'left',
            label: 'Documentation',
          },
          {
            href: 'https://github.com/disneystreaming/smithy4s',
            label: 'GitHub',
            position: 'right',
          },
          {
            href: 'https://discord.gg/wvVga94s8r',
            label: 'Discord',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Get Started',
                to: '/docs/overview/intro',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'GitHub',
                href: 'https://github.com/disneystreaming/smithy4s',
              },
            ],
          },
          {
            title: 'Special thanks',
            items: [
              {
                html: `
                    <a href="https://www.yourkit.com/" target="_blank" rel="noreferrer noopener" aria-label="Profiled with yourkit">
                      <img src="https://www.yourkit.com/images/yk_logo.svg" alt="Profiled with yourkit" />
                    </a>
                  `
              }
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Disney Streaming`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['java', 'scala', 'kotlin'],
      },
    }),
};

module.exports = config;
