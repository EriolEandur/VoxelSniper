package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;

/**
 * http://www.voxelwiki.com/minecraft/VoxelSniper#The_Erosion_Brush
 *
 * @author Piotr
 * @author MikeMatrix
 */
public class RuinBrush extends Brush
{
    private final int ruiningPower = 1;
    private final List<MovingBlock> movingBlocks = new ArrayList<MovingBlock>();
    
    private int[] protectPanes = new int[]{0,0};
    private int[] protectFences = new int[]{0,0};
    
    private double slabChance = 0.2;
    private double stairChance = 0.1;
    private double fullChance = 0.7;
    private double ruinChance = 1;
    
    private RuinMode mode = RuinMode.ALL;
    
    public RuinBrush()
    {
        this.setName("Ruin");
    }

    private void ruin(final SnipeData v)
    {
try{
        movingBlocks.clear();
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize, 2);
        final Undo undo = new Undo();

        for (int y = (brushSize + 1) * 2; y >= 0; y--)
        {
            final double ySquared = Math.pow(y - brushSize, 2);

            for (int x = (brushSize + 1) * 2; x >= 0; x--)
            {
                final double xSquared = Math.pow(x - brushSize, 2);

                for (int z = (brushSize + 1) * 2; z >= 0; z--)
                {
                    if (Math.random()>0.3 && ((xSquared + Math.pow(z - brushSize, 2) + ySquared) <= brushSizeSquared))
                    {
                        final int blockX = this.getTargetBlock().getX()+(x-brushSize);
                        final int blockY = this.getTargetBlock().getY()-(y-brushSize);
                        final int blockZ = this.getTargetBlock().getZ()+(z-brushSize);
                        final int blockId = this.getBlockIdAt(blockX, blockY, blockZ);
                        final byte blockDv = this.getBlockDataAt(blockX, blockY, blockZ);
//Logger.getGlobal().info("Test block: "+blockX+" "+blockY+" "+blockZ+" "+blockId+" "+blockDv);
                        if (!isDurable(blockId,blockDv) 
                                && isMoveable(blockX,blockY,blockZ) 
                                && isAffected(v, blockX,blockY,blockZ)
                                && Math.random()<=ruinChance)                            
                        {
//Logger.getGlobal().info("creae Moving block: "+blockX+" "+blockY+" "+blockZ+" "+blockId+" "+blockDv);
                            movingBlocks.addAll(createMovingBlocks(undo, blockX,blockY,blockZ,blockId,blockDv,ruiningPower));
                        }
                    }
                }
            }
        }
        this.handleMovingBlocks(undo);
        
        //now handle single floating blocks:
        for (int y = (brushSize + 1) * 2; y >= 0; y--)
        {
            final double ySquared = Math.pow(y - brushSize, 2);

            for (int x = (brushSize + 1) * 2; x >= 0; x--)
            {
                final double xSquared = Math.pow(x - brushSize, 2);

                for (int z = (brushSize + 1) * 2; z >= 0; z--)
                {
                    if (((xSquared + Math.pow(z - brushSize, 2) + ySquared) <= brushSizeSquared)){
                        final int blockX = this.getTargetBlock().getX()+(x-brushSize);
                        final int blockY = this.getTargetBlock().getY()-(y-brushSize);
                        final int blockZ = this.getTargetBlock().getZ()+(z-brushSize);
                        if(this.getBlockIdAt(blockX-1, blockY, blockZ)==0
                                && this.getBlockIdAt(blockX+1, blockY, blockZ)==0
                                && this.getBlockIdAt(blockX, blockY-1, blockZ)==0
                                && this.getBlockIdAt(blockX, blockY+1, blockZ)==0
                                && this.getBlockIdAt(blockX, blockY, blockZ-1)==0
                                && this.getBlockIdAt(blockX, blockY, blockZ+1)==0) {
                            final int blockId = this.getBlockIdAt(blockX, blockY, blockZ);
                            final byte blockDv = this.getBlockDataAt(blockX, blockY, blockZ);
                            if(isAffected(v, blockX,blockY,blockZ)) {
                                movingBlocks.addAll(createMovingBlocks(undo, blockX,blockY,blockZ,blockId,blockDv,ruiningPower));
                            }
                        }
                    }
                }
            }
        }
        v.owner().storeUndo(undo);
} catch(Exception e) {
    Logger.getGlobal().log(Level.SEVERE, "Error", e);
}
    }

    // Breakup data for full blocks into slabs {fullID, fullDV, slabID, slabDV}
    private final int[][] slabs = new int[][]{{24,-1,44,1},{4,-1,44,3},{45,-1,44,4},{98,0,44,5},{112,-1,44,6},
                                              {88,-1,44,7},{5,4,126,4},{5,5,126,5},{179,-1,182,0},{201,0,205,0}};
        
    // Breakup data for full blocks into stairs {fullID, fullDV, stairID} 
    // DV of stairs is always 0-3
    private final int[][] stairs = new int[][]{{4,0,67},{5,4,163},{5,5,164},{45,0,108},{98,0,109},{112,0,114},
                                               {24,-1,128},{179,-1,180},{201,0,203}};
    
    private List<MovingBlock> createMovingBlocks(Undo undo, int x, int y, int z, int id, byte dv, int speed) {
        List<MovingBlock> result = new ArrayList<MovingBlock>();
        boolean stableBlock = isStable(x,y,z);
        undo.put(this.clampY(x, y, z));
        setReplacementBlock(x,y,z);
        if(!stableBlock) {
//Logger.getGlobal().info("NotStable!");
            return result;
        }
        for(int i[]: slabs) {
            if(id==i[2]&&dv>7) {
                dv-=8;
                break;
            }
        }
        for(int i[]: stairs) {
            if(id==i[2] && dv>3) {
                dv-=4;
                break;
            }
        }
        double random = Math.random();
        if(random<fullChance) {
//Logger.getGlobal().info("FullBlock");
            result.add(new MovingBlock(x,y,z,id,dv,speed));
            return result;
        }
        if(random<fullChance+slabChance) {
//Logger.getGlobal().info("Slab!");
            for(int[] i: slabs) {
                if(id==i[0]&& (dv==i[1] || i[1]==-1)) {
                    result.add(new MovingBlock(x,y,z,i[2],(byte)i[3],speed));
                    result.add(new MovingBlock(x,y,z,i[2],(byte)i[3],speed));
                    return result;
                }
            }
        }
        for(int[] i:stairs) {
                if(id==i[0] && (dv==i[1] || i[1]==-1)) {
                    byte newDv;
                    if(random<fullChance+slabChance+stairChance/4) {
                        newDv = (byte) 0;
                    } else if(random <fullChance+slabChance+stairChance/2) {
                        newDv = (byte) 1;
                    } else if(random <fullChance+slabChance+stairChance*3/4) {
                        newDv = (byte) 2;
                    } else {
                        newDv = (byte) 3;
                    }
//Logger.getGlobal().info("Stairs!");
                    result.add(new MovingBlock(x,y,z,i[2],newDv,speed));
                    return result;
                }
        }
//Logger.getGlobal().info("FullBlock!");
        result.add(new MovingBlock(x,y,z,id,dv,speed));
        return result;
    }
    
    private final int[] panes = new int[]{102,160};
    private final int[] fences = new int[]{84,113,188,189,190,191,192};
    
    private void setReplacementBlock(int x, int y, int z){
        int id = this.getBlockIdAt(x, y, z);
        byte dv = this.getBlockDataAt(x, y, z);
        if(!isStable(x,y,z)) {
            this.setBlockIdAndDataAt(x, y, z, 0, (byte) 0);
            return;
        }
        if(checkNearbyBlocksFor(x, y, z, panes)) {
            this.setBlockIdAndDataAt(x, y, z, this.protectPanes[0], (byte) this.protectPanes[1]);
        } else if(checkNearbyBlocksFor(x, y, z, fences)) {
            this.setBlockIdAndDataAt(x, y, z, this.protectFences[0], (byte) this.protectFences[1]);
        } else {
            this.setBlockIdAndDataAt(x, y, z, 0, (byte) 0);
        }
    }
    
    private boolean checkNearbyBlocksFor(int x, int y, int z, int[] data) {
        for(int i:data) {
            if(this.getBlockIdAt(x+1, y, z)==i) {
                return true;
            }
            if(this.getBlockIdAt(x-1, y, z)==i) {
                return true;
            }
            if(this.getBlockIdAt(x, y, z+1)==i) {
                return true;
            }
            if(this.getBlockIdAt(x, y, z-1)==i) {
                return true;
            }
        }
        return false;
    }
    
    private void handleMovingBlocks(Undo undo) {
//Logger.getGlobal().info("Moving blocks: "+movingBlocks.size());
        for(MovingBlock block: movingBlocks) {
//Logger.getGlobal().info("Moving block: "+block.blockX+" "+block.blockY+" "+block.blockZ+" "+block.speed);
            while(block.speed>0 || canFall(block)) {
                while(canFall(block)) {
                    block.blockY --;
                    block.speed ++;
                }
                while(block.speed>0) {
                    List<MovingBlock> locations = getAvailableLocations(block);
                    if(locations.isEmpty()) {
                        block.speed=0;
                    } else {
                        double rand = Math.random();
                        int i = new Double(Math.floor(rand*(locations.size()-0.001))).intValue();
//Logger.getGlobal().info(""+locations.size()+" "+i+" "+" "+rand+" "+(i<locations.size()?"ok":"ERROR"));
                        block.blockX = locations.get(i).blockX;
                        block.blockZ = locations.get(i).blockZ;
                        block.speed --;
                    }
                }
            }
            if(!isSupport(block.blockX,block.blockY-1,block.blockZ)) {
//Logger.getGlobal().info("Merged below");
                block.blockY = block.blockY-1;
            }
//Logger.getGlobal().info("Place block!! ");
            undo.put(this.clampY(block.blockX, block.blockY, block.blockZ));
            this.setBlockIdAndDataAt(block.blockX, block.blockY, block.blockZ, block.blockId, block.blockDv);
        }
        movingBlocks.clear();
//Logger.getGlobal().info("Moving blocks finished!! ");
    }
    
    private List<MovingBlock> getAvailableLocations(MovingBlock block) {
        List<MovingBlock> result = new ArrayList<MovingBlock>();
        if(canFall(block)) {
            result.add(new MovingBlock(block.blockX,block.blockY-1,block.blockZ,0,(byte)0,0));
        }
        if(isFree(block.blockX+1,block.blockY,block.blockZ)) {
            result.add(new MovingBlock(block.blockX+1,block.blockY,block.blockZ,0,(byte)0,0));
        }
        if(isFree(block.blockX-1,block.blockY,block.blockZ)) {
            result.add(new MovingBlock(block.blockX-1,block.blockY,block.blockZ,0,(byte)0,0));
        }
        if(isFree(block.blockX,block.blockY,block.blockZ+1)) {
            result.add(new MovingBlock(block.blockX,block.blockY,block.blockZ+1,0,(byte)0,0));
        }
        if(isFree(block.blockX,block.blockY,block.blockZ-1)) {
            result.add(new MovingBlock(block.blockX,block.blockY,block.blockZ-1,0,(byte)0,0));
        }
        return result;
    }
    
    private boolean isAffected(SnipeData v, int x, int y, int z) {
        int id = this.getBlockIdAt(x, y, z);
        byte dv = this.getBlockDataAt(x, y, z);
//Logger.getGlobal().info("id "+id+" dv "+dv);
        /*if(((id==this.protectPanes[0]) && (dv == (byte) this.protectPanes[1]))
                || ((id==this.protectFences[0]) && (dv == (byte) this.protectFences[1]))) {
            return false;
        }*/
//Logger.getGlobal().info("test mode "+mode.name());
        switch(mode) {
            case MATERIAL:
                return id == v.getReplaceId();
            case INK:
                return dv == v.getReplaceData();
            case COMBO:
                return id == v.getReplaceId() && dv == v.getReplaceData();
            case INCLUDE_LIST: 
                return v.getVoxelList().contains(new int[]{id,dv});
            case EXCLUDE_LIST: 
                return !v.getVoxelList().contains(new int[]{id,dv});
            default: 
                return true;
        }
    }
    
    private boolean isMoveable(int x, int y, int z) {
        return !getAvailableLocations(new MovingBlock(x, y, z, 0, (byte) 0, 0)).isEmpty();
    }
    private boolean canFall(MovingBlock block) {
        return isFree(block.blockX,block.blockY-1,block.blockZ);
    }
    
    private boolean isFree(int x, int y, int z) {
        return y>0 && !isStable(x,y,z);
    }
    
    private boolean isDurable(int id, byte dv) {
        switch(id) {
            case 7:
                return true;
        }
        return false;
    }
    
    //IDs of full blocks that can support moving blocks.
    private final int[] support = new int[]{1,2,3,4,12,13,14,15,16,19,21,24,45,48,49,56,67,73,88,98,
                                  108,109,110,112,114,128,129,153,155,159,162,164,168,169,
                                  172,173,179,180,201,202,203,206,214,215,
                                  97};
    // more full blocks {ID,DV}
    private final int[][] support2 = new int[][]{{5,4},{5,5},{19,0}};
        
    private boolean isSupport(int x, int y, int z) {
        int id = this.getBlockIdAt(x, y, z);
        byte dv = this.getBlockDataAt(x, y, z);
        if(isDurable(id, dv)){
            return true;
        }
        for(int i:support) {
            if(id==i) {
                return true;
            }
        }
        for(int[] i:support2) {
            if(id==i[0] && dv == i[1]) {
                return true;
            }
        }
        return false;
    }
    
    //blocks that cannot support other blocks but also cant be destroyed by moving blocks.
    private final int[] stable = new int[]{43,44,125,126,181,204,205, 139};
    
    private boolean isStable(int x, int y, int z) {
        if(isSupport(x,y,z)) {
            return true;
        }
        int id = this.getBlockIdAt(x, y, z);
        byte dv = this.getBlockDataAt(x, y, z);
        for(int i:stable) {
            if(id==i) {
                return true;
            }
        }
        return false;
    }
    
    private class MovingBlock {
        
        int blockX, blockY, blockZ, blockId, speed;
        byte blockDv;
        
        public MovingBlock(int blockX, int blockY, int blockZ, int blockId, byte blockDv, int speed) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.blockId = blockId;
            this.blockDv = blockDv;
            this.speed = speed;
        }
        
    }
    
    @Override
    protected final void arrow(final SnipeData v)
    {
        this.ruin(v);
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.ruin(v);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        boolean chanceChanged = false; 
        
        for (int i = 1; i < par.length; i++)
        {
            final String parameter = par[i];

            
            if (parameter.equalsIgnoreCase("info"))
            {
                v.sendMessage(ChatColor.GOLD + "Ruin Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b ru n -- will include all block on the voxel list");
                v.sendMessage(ChatColor.AQUA + "/b ru x -- will exclude all block on the voxel list");
                v.sendMessage(ChatColor.AQUA + "/b ru m -- will affect voxel replace material");
                v.sendMessage(ChatColor.AQUA + "/b ru i -- will affect voxel replace ink");
                v.sendMessage(ChatColor.AQUA + "/b ru c -- will affect voxel replace combo");
                v.sendMessage(ChatColor.AQUA + "/b ru a -- will affect all blocks");
                v.sendMessage(ChatColor.AQUA + "/b ru p# -- will set block id to preserve glass pane shape");
                v.sendMessage(ChatColor.AQUA + "/b ru f# -- will set block id to preserve fence shape");
                v.sendMessage(ChatColor.AQUA + "/b ru s# -- will set chance for stair formation ");
                v.sendMessage(ChatColor.AQUA + "/b ru h# -- will set chance for half slab formation");
                v.sendMessage(ChatColor.AQUA + "/b ru r# -- will set chance for a block to get ruined");
                return;
            }
            else if (RuinMode.contains(parameter.toLowerCase().charAt(0)))
            {
                mode = RuinMode.getRuinMode(parameter.toLowerCase().charAt(0));
                v.sendMessage(ChatColor.AQUA + "Using "+mode.getDescription()+" mode");
            }
            else if (parameter.startsWith("s"))
            {
                try {
                    this.stairChance = percentage(Integer.parseInt(parameter.replace("s", "")));
                    this.slabChance = Math.min(slabChance, 1-stairChance);
                    this.fullChance = 1-stairChance-slabChance;
                    chanceChanged = true;
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "Stair chance isn't a number.");
                }
             }
            else if (parameter.startsWith("h"))
            {
                try {
                    this.slabChance = percentage(Integer.parseInt(parameter.replace("h", "")));
                    this.stairChance = Math.min(stairChance, 1-slabChance);
                    this.fullChance = 1-stairChance-slabChance;
                    chanceChanged = true;
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "Slab chance isn't a number.");
                }
             }
            else if (parameter.startsWith("r"))
            {
                try {
                    this.ruinChance = percentage(Integer.parseInt(parameter.replace("r", "")));
                    v.sendMessage(ChatColor.AQUA + "Ruining "+(this.ruinChance*100)+"% of all blocks");
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "Ruin chance isn't a number.");
                }
             }
            else if (parameter.startsWith("p"))
            {
                try {
                    this.protectPanes= getIdAndDV(parameter.replace("p", ""));
                    v.sendMessage(ChatColor.AQUA + "Using block ID"+this.protectPanes[0]+":"
                                                                   +this.protectPanes[1]+" to preserve glass panes");
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "ID or ID:DV needed.");
                }
             }
            else if (parameter.startsWith("f"))
            {
                try {
                    this.protectFences= getIdAndDV(parameter.replace("f", ""));
                    v.sendMessage(ChatColor.AQUA + "Using block ID "+this.protectFences[0]+":"
                                                                   +this.protectFences[1]+" to preserve fences");
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "ID or ID:DV needed.");
                }
             }
            else
            {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! use the info parameter to display parameter info.");
            }
        }
        if(chanceChanged) {
            v.sendMessage(ChatColor.AQUA + "Stair formation set to " + (this.stairChance*100)+"%");
            v.sendMessage(ChatColor.AQUA + "Slab formation set to " + (this.slabChance*100)+"%");
        }
    }
    
    private int[] getIdAndDV(String data) throws NumberFormatException {
        int[] result = new int[2];
        String[] splitData = data.split(":");
        result[0] = Integer.parseInt(splitData[0]);
        if(splitData.length>1) {
            result[1] = Integer.parseInt(splitData[1]);
        }
        return result;
    }
    
    private double percentage(int value) {
        return Math.min(1, Math.max(0, value/10000.0));
    }
    
    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.size();
        vm.brushMessage(ChatColor.AQUA + "Using "+mode.getDescription()+" mode");
        vm.brushMessage(ChatColor.AQUA + "Ruining "+(this.ruinChance*100)+"% of all blocks");
        vm.brushMessage(ChatColor.AQUA + "Stair formation set to " + (this.stairChance*100)+"%");
        vm.brushMessage(ChatColor.AQUA + "Slab formation set to " + (this.slabChance*100)+"%");
        vm.brushMessage(ChatColor.AQUA + "Using block ID "+this.protectPanes[0]+":"
                                                       +this.protectPanes[1]+" to preserve glass panes");
        vm.brushMessage(ChatColor.AQUA + "Using block ID "+this.protectFences[0]+":"
                                                       +this.protectFences[1]+" to preserve fences");
        
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.ruin";
    }
    
    private enum RuinMode {
        INCLUDE_LIST ('n',"include list"),
        EXCLUDE_LIST  ('x',"exclude list"),
        ALL  ('a',"replace all"),
        MATERIAL  ('m',"replace material"),
        INK ('i',"replace ink"),
        COMBO ('c',"replace combo");
        
        private final char mode;
        
        private final String description;
        
        private RuinMode(char mode, String description) {
            this.mode = mode;
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static RuinMode getRuinMode(char letter) {
            for(RuinMode type: RuinMode.values()) {
                if(letter == type.mode) {
                    return type;
                }
            }
            return RuinMode.ALL;
        }
        
        public static boolean contains(char letter) {
            for(RuinMode type: RuinMode.values()) {
                if(letter == type.mode) {
                    return true;
                }
            }
            return false;
        }
    }
    

}
