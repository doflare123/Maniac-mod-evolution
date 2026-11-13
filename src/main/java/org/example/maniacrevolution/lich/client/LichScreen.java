package org.example.maniacrevolution.lich.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.lich.LichRules;
import org.lwjgl.glfw.GLFW;

/** The open command book is RTS control mode. Closing restores every hotbar key. */
public final class LichScreen extends Screen {
    private CompoundTag data;
    private java.util.UUID corpse;
    private boolean dirty;
    private int left, top, panelWidth;
    public LichScreen(CompoundTag tag) { super(Component.literal("Книга Лича")); data = tag.copy(); if (tag.hasUUID("corpse")) corpse = tag.getUUID("corpse"); }
    public boolean armyMode() { return data.getString("mode").equals("army"); }
    public void update(CompoundTag tag) { data = tag.copy(); dirty = true; }
    @Override public boolean isPauseScreen() { return false; }
    private void button(String label, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,18).build());
    }
    private static CompoundTag tag(String key, int value) { var d = new CompoundTag(); d.putInt(key,value); return d; }
    @Override protected void init() {
        clearWidgets(); panelWidth = Math.min(480,width-16); left = (width-panelWidth)/2; top = Math.max(8,(height-320)/2);
        var rows = data.getList("rows",Tag.TAG_COMPOUND);
        if (data.getString("mode").equals("editor")) {
            for (int i=0;i<rows.size();i++) {
                var row = rows.getCompound(i); int y=top+30+i*44;
                button((row.getString("id").equals(data.getString("map"))?"✓ ":"")+row.getString("name")+" · "+row.getInt("points")+" точек",left+8,y,panelWidth-120,()-> {
                    var d=new CompoundTag();d.putString("map",row.getString("id"));LichClient.request("map",d);
                });
                button("Трупы / точки",left+panelWidth-108,y,100,()->list(row.getString("id"),0));
            }
            return;
        }
        if (data.getString("mode").equals("corpses")) {
            for(int i=0;i<rows.size();i++) {
                var row=rows.getCompound(i); if(row.getBoolean("point")) button("×",left+panelWidth-26,top+28+i*18,18,()-> {
                    var d=new CompoundTag();d.putUUID("id",row.getUUID("id"));LichClient.request("delete_point",d); list(data.getString("map"),data.getInt("page"));
                });
            }
            paging(top+250,false); button("Карты",left+80,top+250,70,()->LichClient.request("editor"));return;
        }
        if (corpse != null) {
            for (var kind:LichRules.Kind.values()) button(kind.title+" · "+kind.cost+" маны",left+12,top+35+kind.ordinal()*24,panelWidth-24,()->cast("raise",kind.ordinal()));
            button("Поглотить · +20 маны",left+12,top+140,panelWidth-24,()->cast("absorb",0));
            button("Армия",left+12,top+170,90,()-> {corpse=null;init();});return;
        }
        for(int i=0;i<rows.size();i++) {
            var row=rows.getCompound(i); button((row.getBoolean("selected")?"✓ ":"")+row.getString("name")+" · "+(int)row.getFloat("hp")+" HP · "+row.getInt("distance")+"м · "+row.getString("order")+(row.getBoolean("attack")?" [бой]":" [приказ]"),left+8,top+28+i*16,panelWidth-16,()-> {
                var d=new CompoundTag();d.putUUID("id",row.getUUID("id"));d.putBoolean("add",hasShiftDown());LichClient.request("select",d);
            });
        }
        int y=top+226; paging(y,true);
        button("Все",left+120,y,48,()->LichClient.request("all"));
        button("Ремонт · 5",left+174,y,94,()->LichClient.request("repair"));
        button("Держать приказ",left+274,y,Math.max(70,panelWidth-282),()->reaction(false));
        button("Атаковать до конца",left+8,y+22,140,()->reaction(true));
        int i=0;
        for(var order:LichRules.Order.values()) {
            int col=i%4, row=i/4; i++;
            button(order.title,left+8+col*(panelWidth-16)/4,y+46+row*20,(panelWidth-20)/4,()-> {
                LichClient.request("order",tag("order",order.ordinal()));
                if (order==LichRules.Order.CHASE || order==LichRules.Order.GUARD || order==LichRules.Order.MOVE) onClose();
            });
        }
    }
    private void reaction(boolean attack) {var d=new CompoundTag();d.putBoolean("attack",attack);LichClient.request("reaction",d);}
    private void cast(String action,int kind) {var d=tag("kind",kind);d.putUUID("corpse",corpse);LichClient.request(action,d);onClose();}
    private void list(String map,int page) {var d=tag("page",page);d.putString("map",map);LichClient.request("corpse_list",d);}
    private void paging(int y,boolean army) {
        button("‹",left+8,y,26,()-> {int p=Math.max(0,data.getInt("page")-1);if(army)LichClient.request("page",tag("page",p));else list(data.getString("map"),p);});
        button("›",left+40,y,26,()-> {int p=Math.min(data.getInt("pages")-1,data.getInt("page")+1);if(army)LichClient.request("page",tag("page",p));else list(data.getString("map"),p);});
    }
    @Override public void tick() { if(dirty){dirty=false;init();} }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(armyMode() && key>=GLFW.GLFW_KEY_0 && key<=GLFW.GLFW_KEY_9) {
            var d=tag("key",key-GLFW.GLFW_KEY_0);d.putBoolean("save",hasControlDown());LichClient.request("group",d);return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean mouseScrolled(double x,double y,double amount) {
        if(armyMode() && corpse==null){int p=Math.max(0,Math.min(data.getInt("pages")-1,data.getInt("page")+(amount>0?-1:1)));LichClient.request("page",tag("page",p));return true;}
        return super.mouseScrolled(x,y,amount);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);g.fill(left,top,left+panelWidth,Math.min(height,top+320),0xf01b102b);
        g.drawCenteredString(font,"§dКнига Лича · "+(armyMode()?"Ctrl+0–9: запись; 0–9: группа":data.getString("map")),width/2,top+9,0xffffff);
        if(data.getString("mode").equals("corpses")){var rows=data.getList("rows",Tag.TAG_COMPOUND);for(int i=0;i<rows.size();i++)g.drawString(font,font.plainSubstrByWidth(rows.getCompound(i).getString("text"),panelWidth-40),left+8,top+32+i*18,0xd2bfdd,false);}
        super.render(g,mouseX,mouseY,partial);
    }
}
