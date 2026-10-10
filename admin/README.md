# lnb Admin

A simplified admin system based on [Art Design Pro](https://github.com/Daymychen/art-design-pro).

## Original Project

- Repository: https://github.com/Daymychen/art-design-pro
- Documentation: https://www.artd.pro/docs
- Demo: https://www.artd.pro

## Simplifications

This version has been simplified from the original:

- Menu reduced to: Dashboard Console, System Management (User, Role)
- Removed login page slider verification and role dropdown
- Removed header bar quick entry, notifications, ArtBot chat
- Simplified user menu to only show username and logout
- Removed about project section from dashboard

Other example pages are moved to `src/views/_examples/` for reference and won't be bundled in production.

## Tech Stack

- Vue 3 + TypeScript + Vite
- Element Plus + Tailwind CSS
- Pinia + Vue Router

## Quick Start

```bash
# Install dependencies
pnpm install

# Start dev server
pnpm dev

# Build for production
pnpm build
```

## Default Account

- Username: Super
- Password: 123456

## License

[MIT](./LICENSE)
