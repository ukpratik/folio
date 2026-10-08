// Five icon directions. Each: bg (full 108×108 layer), fg (artwork inside the 66dp safe zone), mono (themed-icon silhouette).
const T = '#0D6B62', TD = '#0A524B', M = '#6FD3C4', P = '#FFFFFF', INK = '#0A524B';
const grad = (id) => `<defs><linearGradient id="${id}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#11867A"/><stop offset="1" stop-color="${TD}"/></linearGradient></defs>`;
const page = (x, y, w, h, ear, fill = P) =>
  `<path d="M${x + 3},${y}h${w - ear - 3}l${ear},${ear}v${h - ear - 3}a3,3 0 0 1 -3,3h${-(w - 6)}a3,3 0 0 1 -3,-3v${-(h - 6)}a3,3 0 0 1 3,-3z" fill="${fill}"/>` +
  `<path d="M${x + w - ear},${y}v${ear - 2}a2,2 0 0 0 2,2h${ear - 2}" fill="${M}" opacity=".9"/>`;
const lines = (x, y, w, gap, n, c = '#C9D8D5') => Array.from({ length: n }, (_, i) =>
  `<rect x="${x}" y="${y + i * gap}" width="${i === n - 1 ? w * 0.6 : w}" height="2.6" rx="1.3" fill="${c}"/>`).join('');

window.ICONS = [
  {
    key: 'A', name: 'Scan Frame', tagline: 'A clean page inside scanner crop corners',
    bg: `${grad('ga')}<rect width="108" height="108" fill="url(#ga)"/>`,
    fg: `${page(42, 34, 24, 34, 7)}${lines(46, 46, 16, 6, 3)}
      <g fill="none" stroke="${M}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
        <path d="M34,38v-8h8"/><path d="M66,30h8v8"/><path d="M74,64v8h-8"/><path d="M42,72h-8v-8"/></g>`,
    mono: `<path d="M45,34h14l7,7v24a3,3 0 0 1 -3,3h-18a3,3 0 0 1 -3,-3v-28a3,3 0 0 1 3,-3z" fill="#000"/>
      <g fill="none" stroke="#000" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
        <path d="M34,38v-8h8"/><path d="M66,30h8v8"/><path d="M74,64v8h-8"/><path d="M42,72h-8v-8"/></g>`,
  },
  {
    key: 'B', name: 'Folio F', tagline: 'A page whose folds form a bold “F” monogram',
    bg: `${grad('gb')}<rect width="108" height="108" fill="url(#gb)"/>`,
    fg: `${page(36, 30, 36, 48, 10)}
      <path d="M46,42h17v6.5h-10v5h8.5v6.5h-8.5v10h-7z" fill="${T}"/>`,
    mono: `<path fill-rule="evenodd" fill="#000" d="M39,30h23l10,10v35a3,3 0 0 1 -3,3h-30a3,3 0 0 1 -3,-3v-42a3,3 0 0 1 3,-3z
      M46,42v28h7v-10h8.5v-6.5h-8.5v-5h10v-6.5z"/>`,
  },
  {
    key: 'C', name: 'Fit to Size', tagline: 'Arrows pressing a page down to size, the app’s superpower',
    bg: `${grad('gc')}<rect width="108" height="108" fill="url(#gc)"/>`,
    fg: `${page(41, 31, 26, 36, 7)}${lines(45, 43, 18, 6, 3)}
      <g fill="none" stroke="${M}" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M27,49l7,5l-7,5"/><path d="M81,49l-7,5l7,5"/></g>
      <rect x="44" y="73" width="20" height="5" rx="2.5" fill="${M}"/>`,
    mono: `<path d="M44,31h16l7,7v26a3,3 0 0 1 -3,3h-20a3,3 0 0 1 -3,-3v-30a3,3 0 0 1 3,-3z" fill="#000"/>
      <g fill="none" stroke="#000" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M27,49l7,5l-7,5"/><path d="M81,49l-7,5l7,5"/></g><rect x="44" y="73" width="20" height="5" rx="2.5" fill="#000"/>`,
  },
  {
    key: 'D', name: 'Private Page', tagline: 'A document protected by a shield, for “never leaves your phone”',
    bg: `${grad('gd')}<rect width="108" height="108" fill="url(#gd)"/>`,
    fg: `${page(36, 29, 30, 42, 8)}${lines(41, 41, 18, 6, 3)}
      <path d="M66,52l11,4v8c0,7.5 -4.8,12.5 -11,15c-6.2,-2.5 -11,-7.5 -11,-15v-8z" fill="${M}" stroke="${TD}" stroke-width="3" stroke-linejoin="round"/>
      <path d="M61.5,65l3.2,3.2l6,-6.2" fill="none" stroke="${TD}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`,
    mono: `<path d="M39,29h19l8,8v20l-11,4v3c0,2.4 .5,4.6 1.4,6.6h-17.4a3,3 0 0 1 -3,-3v-36a3,3 0 0 1 3,-3z" fill="#000"/>
      <path d="M66,52l11,4v8c0,7.5 -4.8,12.5 -11,15c-6.2,-2.5 -11,-7.5 -11,-15v-8z" fill="#000"/>`,
  },
  {
    key: 'E', name: 'Folio Stack', tagline: 'Two pages fanned like a folio, with a done tick',
    bg: `<rect width="108" height="108" fill="#E8F4F1"/>`,
    fg: `<g transform="rotate(-10 54 54)"><rect x="34" y="30" width="30" height="42" rx="3.5" fill="${M}"/></g>
      ${page(44, 32, 30, 42, 8)}${lines(49, 44, 18, 6, 3, '#D3DFDC')}
      <circle cx="70" cy="70" r="10" fill="${T}"/>
      <path d="M65.5,70.2l3.2,3.2l6,-6.2" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`,
    mono: `<g transform="rotate(-10 54 54)"><rect x="34" y="30" width="30" height="42" rx="3.5" fill="#000" opacity=".55"/></g>
      <path d="M47,32h19l8,8v19a12,12 0 0 0 -12.5,15h-14.5a3,3 0 0 1 -3,-3v-36a3,3 0 0 1 3,-3z" fill="#000"/><circle cx="70" cy="70" r="10" fill="#000"/>`,
  },
];
