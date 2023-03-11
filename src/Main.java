import net.querz.io.MaxDepthReachedException;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "nbtmcafix", description = "Finds and fixes players and entities with broken (too deep) NBT tags.")

public class Main implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Path to `world` directory.")
    private String basePath;

    @CommandLine.Option(names = { "-p", "--players_depth" }, description = "Maximum allowed depth of players' NBT tags (default = 512).")
    private int playerDepth = 512;

    @CommandLine.Option(names = { "-e", "--entities_depth" }, description = "Maximum allowed depth of entities' NBT tags (default = 512).")
    private int entityDepth = 512;

    @Override
    public Integer call() throws Exception {
        Path playerdataPath = Paths.get(basePath, "playerdata");
        Path entitiesPath = Paths.get(basePath, "entities");

        File playerdataDir = new File(playerdataPath.toString());
        File entitiesDir = new File(entitiesPath.toString());

        File[] playerdataFiles = playerdataDir.listFiles();
        File[] entitiesFiles = entitiesDir.listFiles();

        if (playerdataFiles != null && entitiesFiles != null) {
            for (File player : playerdataFiles) {
                fixPlayer(player, playerDepth);
            }
            for (File entity : entitiesFiles) {
                fixChunks(entity, entityDepth);
            }
        } else {
            System.out.println("Invalid directories");
            return 1;
        }
        return 0;
    }

    static boolean findBadTags(Tag<?> tag, File file, int maxDepth) {
        CompoundTag compoundTag = (CompoundTag) tag;
        CompoundTag nestedCompoundTag;
        ListTag<?> nestedListTag;
        Set<String> keys = compoundTag.keySet();
        String[] keyArray = keys.toArray(new String[0]);
        boolean save = false;

        for (int i = 0; i < keys.size(); i++) {
            try {
                nestedCompoundTag = compoundTag.getCompoundTag(keyArray[i]);
                save |= findBadTags(nestedCompoundTag, file, maxDepth);
            } catch (ClassCastException e) {
                try {
                    nestedListTag = compoundTag.getListTag(keyArray[i]);
                    try {
                        nestedListTag.checkDepth(maxDepth);
                    } catch (MaxDepthReachedException e2) {
                        System.out.println("\tMax depth reached in object " + keyArray[i]);
                        compoundTag.remove(keyArray[i]);
                        save = true;
                    }
                } catch (ClassCastException ignored) {}
            }
        }

        return save;
    }

    static void fixChunks(File file, int maxDepth) {
        try {
            boolean save = false;
            System.out.println("[" + file.getName() + "] Reading file");
            Chunk chunk = null;
            MCAFile mcafile = MCAUtil.read(file);
            for (int i = 0; i < 1024; i++) {
                try {
                    chunk = mcafile.getChunk(i);
                    chunk.getEntities().checkDepth(maxDepth);
                } catch (MaxDepthReachedException e) {
                    System.out.println("[" + file.getName() + "] Chunk " + i + " contains invalid entities");
                    if (chunk != null) {
                        System.out.println("\tRemoving entities");
                        chunk.getEntities().clear();
                        save = true;
                    }
                } catch (NullPointerException ignored) {}
            }
            if (save) {
                MCAUtil.write(mcafile, file.getPath());
                System.out.println("[" + file.getName() + "] File saved");
            }
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

            if (findBadTags(tag, file, maxDepth)) {
                NBTUtil.write(tag, file.getPath());
                System.out.println("[" + file.getName() + "] File saved");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}