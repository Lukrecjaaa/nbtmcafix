# nbtmcafix

Program which fixes `Tried to read NBT tag with too high complexity, depth > 512` errors like here https://github.com/LunaPixelStudios/Better-MC/issues/1103

## Usage
```
Missing required parameter: '<basePath>'
Usage: nbtmcafix [-e=<entityDepth>] [-p=<playerDepth>] <basePath>
Finds and fixes players and entities with broken (too deep) NBT tags.
      <basePath>   Path to `world` directory.
  -e, --entities_depth=<entityDepth>
                   Maximum allowed depth of entities' NBT tags (default = 512).
  -p, --players_depth=<playerDepth>
                   Maximum allowed depth of players' NBT tags (default = 512).
```

## Dependencies
The program uses [NBT](https://github.com/Querz/NBT) library to fix affected files and [picocli](https://picocli.info/) to parse command line parameters.

The NBT library (commit `04bad909afa99716e2c95d6051d685c2a839e1da`) had to be slightly modified for the program to work, git patch is available in `nbt_library.patch` file.
