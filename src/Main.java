import net.querz.io.MaxDepthReachedException;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class Main {
    static void findBadTags(Tag<?> tag, File file, int maxDepth) {
        CompoundTag compoundTag = (CompoundTag) tag;
        CompoundTag nestedCompoundTag;
        ListTag<?> nestedListTag;
        Set<String> keys = compoundTag.keySet();
        String[] keyArray = keys.toArray(new String[0]);

        for (int i = 0; i < keys.size(); i++) {
            try {
                nestedCompoundTag = compoundTag.getCompoundTag(keyArray[i]);
                findBadTags(nestedCompoundTag, file, maxDepth);
            } catch (ClassCastException e) {
                try {
                    nestedListTag = compoundTag.getListTag(keyArray[i]);
                    try {
                        nestedListTag.valueToString(maxDepth);
                    } catch (MaxDepthReachedException e2) {
                        System.out.println("[" + file.getName() + "] Max depth reached in object " + keyArray[i]);
                        compoundTag.remove(keyArray[i]);
                    }
                } catch (ClassCastException ignored) {}
            }
        }
    }

    static void fixChunks(File file, int maxDepth) {
        try {
            System.out.println("[" + file.getName() + "] Reading file");
            Chunk chunk = null;
            MCAFile mcafile = MCAUtil.read(file);
            for (int i = 0; i < 1024; i++) {
                try {
                    chunk = mcafile.getChunk(i);
                    chunk.getEntities().valueToString(maxDepth);
                } catch (MaxDepthReachedException e) {
                    System.out.println("[" + file.getName() + "] Chunk " + i + " broken");
                    if (chunk != null) {
                        System.out.println("\tRemoving entities");
                        chunk.getEntities().clear();
                    }
                } catch (NullPointerException ignored) {}
            }
            MCAUtil.write(mcafile, file.getPath());
            System.out.println("[" + file.getName() + "] File saved");
        } catch (IOException e) {
            System.out.println("[" + file.getName() + "] File is empty");
        }
    }

    static void fixPlayer(File file, int maxDepth) {
        try {
            System.out.println("[" + file.getName() + "] Reading file");

            CompoundTag tag;
            NamedTag namedTag = NBTUtil.read(file);
            tag = (CompoundTag) namedTag.getTag();

            findBadTags(tag, file, maxDepth);

            NBTUtil.write(tag, file.getPath());

            System.out.println("[" + file.getName() + "] File saved");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Specify path to 'world' directory");
        } else {
            String basePath = args[0];
            Path playerdataPath = Paths.get(basePath, "playerdata");
            Path entitiesPath = Paths.get(basePath, "entities");

            File playerdataDir = new File(playerdataPath.toString());
            File entitiesDir = new File(entitiesPath.toString());

            File[] playerdataFiles = playerdataDir.listFiles();
            File[] entitiesFiles = entitiesDir.listFiles();

            if (playerdataFiles != null && entitiesFiles != null) {
                for (File player : playerdataFiles) {
                    fixPlayer(player, 64);
                }
                for (File entity : entitiesFiles) {
                    fixChunks(entity, 256);
                }
            } else {
                System.out.println("Invalid directories");
            }
        }
    }
}