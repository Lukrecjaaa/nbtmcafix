# nbtmcafix

Program which fixes `Tried to read NBT tag with too high complexity, depth > 512` errors like here https://github.com/LunaPixelStudios/Better-MC/issues/1103

## Usage
```
java -jar ./nbtmcafix.jar ~/minecraft_dir/world
```

## Patch
The program uses [NBT](https://github.com/Querz/NBT) library to fix affected files (commit `04bad909afa99716e2c95d6051d685c2a839e1da`), but it had to be slightly modified. Git patch is available in `nbt_library.patch` file.
