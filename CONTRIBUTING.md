# Contributing to Allium

## Reporting Issues

When reporting an issue please gather:
1. Logs or in-game screenshots to demonstrate the issue.
2. The version of Allium being used, whether that's on modrinth or from this repository directly.
3. If a consistent case is known to reproduce the issue, please provide it in the form of a gist or repository. 
4. If the location of the bug is known, please provide it in the form of a link to the file in the repository, 
highlighting the specific line or set of lines.

## Feature Requests

Feature requests are welcome. Please take care to outline what the feature should do, and how it may be represented in 
Lua, or in a script manifest.

## Pull Requests

If you'd like to discuss contribution, although not required,
please feel free to join the [Moongarden Mods Discord Server](https://discord.gg/rWSaP222G9).

This project has a `.editorconfig` file at the root. 
Please make sure your IDE either supports them outright, 
or includes an extension to ensure proper formatting of code prior to submission.

### Project Structure

The repository is broken up into 2 Gradle subprojects that each build into their own jars: 
- `allium` - The bare-minimum necessary for a Lua script to be run in the game. 
Allium is game version independent, and as such **must not** use any game code.
- `bouquet` - A Lua API for the game. Bouquet targets the latest release version of the game, 
providing quality-of-life libraries and useful hooks into common game logic.

### AI Contributions

This project was started for fun, not for profit, as a hobby, and as a challenge for the mind. 
As such, **generative AI contributions are not welcome**. 
Due to the complexity in detecting generative AI contributions, some may slip through the cracks. 
If it is discovered that a contribution contains code output by a generative AI model, 
it will be dropped, and future contributions by the offending author will be immediately rejected.