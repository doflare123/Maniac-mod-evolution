package org.example.maniacrevolution.lich.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.lich.LichRules;
import org.example.maniacrevolution.mana.ClientManaData;
import org.lwjgl.glfw.GLFW;

/** Army controls share the ritual palette and keep their widgets through snapshots. */
public final class LichArmyScreen extends Screen {
    private static final int INK=0xffe1efdf,JADE=0xff79d6b1,MUTED=0xff92a79d,GOLD=0xffc5ad72;
    private final List<ArmyButton> controls=new ArrayList<>();
    private final long opened=System.nanoTime();
    private CompoundTag data;
    private LichArmyLayout.Frame frame;
    public LichArmyScreen(CompoundTag tag) { super(Component.literal("Книга Лича"));data=tag.copy(); }
    public void update(CompoundTag tag) { data=tag.copy();sync(); }
    @Override public boolean isPauseScreen() { return false; }
    private double seconds() { return (System.nanoTime()-opened)/1_000_000_000.0; }
    private int page() { return data.getInt("page"); }
    private int pages() { return Math.max(1,data.getInt("pages")); }
    private boolean hasSelection() { return data.getInt("selected")>0; }
    private static CompoundTag number(String key,int value) { var tag=new CompoundTag();tag.putInt(key,value);return tag; }
    private void page(int page) { LichClient.request("page",number("page",Math.max(0,Math.min(pages()-1,page)))); }
    private void group(int key) {
        var tag=number("key",key);tag.putBoolean("save",hasControlDown());LichClient.request("group",tag);
    }
    private void reaction(boolean attack) { var tag=new CompoundTag();tag.putBoolean("attack",attack);LichClient.request("reaction",tag); }
    private ArmyButton button(String label,int x,int y,int w,int h,BooleanSupplier enabled,Runnable action) {
        var button=new ArmyButton(label,x,y,w,h,-1,enabled,action);
        controls.add(addRenderableWidget(button));return button;
    }
    private static String orderDescription(LichRules.Order order) {
        return switch(order) {
            case FOLLOW -> "Следуют за Личом, сохраняя небольшую дистанцию. После боя продолжают сопровождение.";
            case STAND -> "Закрепляет текущую позицию каждого юнита. После боя или поиска потерянной цели юниты возвращаются на свои места.";
            case GUARD -> "Охраняют позицию возле выбранного компьютера и реагируют на противников рядом.\nПосле закрытия меню нажмите ПКМ книгой по компьютеру.";
            case PATROL -> "Патрулируют область радиусом 10 блоков вокруг места, где сейчас стоит Лич, и реагируют на замеченных противников.";
            case SEARCH -> "Самостоятельно проверяют районы, где могут находиться выжившие, и атакуют обнаруженных. Район поиска обновляется каждые 10 секунд.";
            case CHASE -> "Преследуют выбранного выжившего. При потере видимости ищут его по последней позиции и направлению движения.\nПосле закрытия меню нажмите ПКМ книгой по видимому выжившему.";
            case RETURN -> "Возвращаются к Личу. После прибытия получают приказ «Стоять» и остаются в этой точке.";
            case MOVE -> "Идут в выбранную точку. После прибытия получают приказ «Стоять».\nПосле закрытия меню нажмите ПКМ книгой по нужному месту.";
        };
    }
    @Override protected void init() {
        clearWidgets();controls.clear();
        for(int i=0;i<12;i++) {
            final int index=i;
            var button=new ArmyButton("Выбрать нежить",36,87+i*19,300,18,i,()->true,()->{
                var rows=data.getList("rows",Tag.TAG_COMPOUND);
                if(index>=rows.size())return;
                var row=rows.getCompound(index);if(!row.hasUUID("id"))return;
                var tag=new CompoundTag();tag.putUUID("id",row.getUUID("id"));tag.putBoolean("add",hasShiftDown());
                LichClient.request("select",tag);
            });
            controls.add(addRenderableWidget(button));
        }
        button("×",584,23,24,22,()->true,this::onClose);
        button("Выбрать всех",372,72,112,24,()->data.getInt("total")>0,()->LichClient.request("all"))
                .setTooltip(Tooltip.create(Component.literal("Выбрать всю свою нежить, включая юнитов на других страницах.")));
        button("Снять выбор",492,72,112,24,this::hasSelection,()->LichClient.request("none"))
                .setTooltip(Tooltip.create(Component.literal("Снять выбор со всей армии. Юниты продолжат выполнять выданные приказы.")));
        int index=0;
        for(var order:LichRules.Order.values()) {
            int column=index%2,row=index/2;index++;
            var button=button(order.title,372+column*120,124+row*29,112,24,this::hasSelection,()->{
                LichClient.request("order",number("order",order.ordinal()));
                if(order==LichRules.Order.CHASE||order==LichRules.Order.GUARD||order==LichRules.Order.MOVE)onClose();
            });
            button.setTooltip(Tooltip.create(Component.literal(orderDescription(order))));
        }
        button("Держать приказ",372,262,112,24,this::hasSelection,()->reaction(false))
                .setTooltip(Tooltip.create(Component.literal("Реагируют на противников рядом с областью приказа. После боя или завершения поиска потерянной цели возвращаются к приказу.")));
        button("До конца",492,262,112,24,this::hasSelection,()->reaction(true))
                .setTooltip(Tooltip.create(Component.literal("Разрешает уходить дальше от точки приказа ради погони за замеченными противниками. При потере следа юниты возвращаются к приказу.")));
        button("Ремонт филактерии",372,302,232,28,
                ()->data.getInt("hp")>0&&data.getInt("hp")<10&&data.getInt("cast")==0&&ClientManaData.getMana()>=5,
                ()->LichClient.request("repair"))
                .setTooltip(Tooltip.create(Component.literal("+1 прочность за 5 маны. Филактерия в руках или рядом")));
        button("‹",36,329,24,21,()->page()>0,()->page(page()-1));
        button("›",144,329,24,21,()->page()<pages()-1,()->page(page()+1));
        for(int key=0;key<10;key++) {
            final int group=key;
            button(Integer.toString(key),140+key*25,354,22,22,()->true,()->group(group))
                    .setTooltip(Tooltip.create(Component.literal("Группа "+key+" · Ctrl + клик — записать выбор")));
        }
        sync();
    }
    private void sync() {
        frame=LichArmyLayout.frame(width,height,seconds());
        var rows=data.getList("rows",Tag.TAG_COMPOUND);
        for(var button:controls) {
            button.setX(frame.screenX(button.localX));button.setY(frame.screenY(button.localY));
            button.setWidth(frame.size(button.localWidth));button.setHeight(frame.size(button.localHeight));
            button.visible=button.row<0||button.row<rows.size();
            button.active=button.visible&&button.enabled.getAsBoolean();
            if(button.row>=0&&button.visible) {
                var row=rows.getCompound(button.row);
                button.setMessage(Component.literal(row.getString("name")+", "+(int)row.getFloat("hp")+" HP, "+row.getString("order")+(row.getBoolean("selected")?", выбрано":"")));
            }
        }
    }
    @Override public void tick() { sync(); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(org.example.maniacrevolution.event.ArmorAbilityKeyHandler.closeLichMenu(this,key,scan))return true;
        if(key>=GLFW.GLFW_KEY_0&&key<=GLFW.GLFW_KEY_9) {group(key-GLFW.GLFW_KEY_0);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT) {onClose();return true;}
        sync();return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double amount) {
        if(amount!=0)page(page()+(amount>0?-1:1));return true;
    }
    private int color(int color) { return (color&0xffffff)|(Math.round((color>>>24)*frame.opacity())<<24); }
    private void begin(GuiGraphics g) {
        g.pose().pushPose();g.pose().translate(frame.x(),frame.y(),0);g.pose().scale((float)frame.scale(),(float)frame.scale(),1);
    }
    private void text(GuiGraphics g,String text,int x,int y,int tint) {g.drawString(font,text,x,y,color(tint),false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        sync();
        g.fillGradient(0,0,width,height,color(0xd9091413),color(0xed030809));
        if(frame.opacity()<=.02f)return;
        begin(g);
        g.fillGradient(16,12,624,391,color(0xf011241e),color(0xf0091013));
        g.renderOutline(16,12,608,379,color(0xff36594c));
        text(g,"КНИГА ЛИЧА",32,27,GOLD);
        text(g,"Нежить: "+data.getInt("total")+"   ·   Выбрано: "+data.getInt("selected"),32,44,MUTED);
        g.fill(28,64,344,323,color(0x9006100e));
        text(g,"АРМИЯ",36,71,GOLD);text(g,"ПРИКАЗЫ",372,109,GOLD);
        text(g,"РЕАКЦИЯ НА ПРОТИВНИКА",372,247,MUTED);
        String feedback=LichWorldMarkers.status();
        if(!feedback.isEmpty())text(g,feedback,372,336,0xffd6ffed);
        if(data.getList("rows",Tag.TAG_COMPOUND).isEmpty()) {
            LichHudIcons.unit(g,1,180,157,frame.opacity());
            g.drawCenteredString(font,"Армия ещё не поднята",186,185,color(INK));
            g.drawCenteredString(font,"Откройте труп книгой",186,202,color(MUTED));
        }
        g.drawCenteredString(font,(page()+1)+" / "+pages(),102,336,color(MUTED));
        text(g,"RTS-ГРУППЫ",36,361,GOLD);
        text(g,"Ctrl + цифра — записать",400,360,MUTED);
        text(g,"Цифра — группа   ·   Shift + клик — добавить   ·   ПКМ / Esc — закрыть",36,379,MUTED);
        g.pose().popPose();
        super.render(g,mx,my,partial);
    }
    private final class ArmyButton extends AbstractButton {
        final int localX,localY,localWidth,localHeight,row;
        final BooleanSupplier enabled;
        final Runnable action;
        ArmyButton(String label,int x,int y,int width,int height,int row,BooleanSupplier enabled,Runnable action) {
            super(0,0,1,1,Component.literal(label));localX=x;localY=y;localWidth=width;localHeight=height;
            this.row=row;this.enabled=enabled;this.action=action;
        }
        @Override public void onPress() {if(active)action.run();}
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            if(frame.opacity()<=.02f)return;
            begin(g);g.pose().translate(localX,localY,0);
            boolean hover=isHoveredOrFocused()&&active;
            CompoundTag unit=row>=0?data.getList("rows",Tag.TAG_COMPOUND).getCompound(row):null;
            boolean selected=unit!=null&&unit.getBoolean("selected");
            int border=hover||selected?JADE:0xff36594c;
            g.fillGradient(0,0,localWidth,localHeight,color(hover||selected?0xff204536:0xff10271f),color(0xff0b1515));
            if(row<0)g.renderOutline(0,0,localWidth,localHeight,color(border));
            else {
                g.fill(0,0,2,localHeight,color(unit.getBoolean("hurt")?0xffde8185:border));
                int pulse=unit.contains("entityId")?LichWorldMarkers.pulseAlpha(unit.getInt("entityId")):0;
                if(pulse>0)g.renderOutline(0,0,localWidth,localHeight,color((pulse<<24)|0xd6ffed));
            }
            if(unit==null) {
                int textX=(localWidth-font.width(getMessage()))/2;
                if(localHeight==28)textX=12;
                g.drawString(font,getMessage(),textX,(localHeight-8)/2,color(active?JADE:0xff52685e),false);
                if(localHeight==28) {
                    text(g,"5",localWidth-27,10,0xff3c9fe8);
                    g.fill(localWidth-14,9,localWidth-11,16,color(0xff3c9fe8));
                    g.fill(localWidth-15,11,localWidth-10,14,color(0xff8bd7ff));
                }
            } else {
                LichHudIcons.unit(g,unit.getInt("kind"),6,3,frame.opacity());
                text(g,unit.getString("name"),25,5,selected?JADE:INK);
                String hp=Integer.toString(Math.max(0,(int)Math.ceil(unit.getFloat("hp"))));
                text(g,hp,97,5,INK);LichHudIcons.heart(g,100+font.width(hp),6,color(0xffde8185));
                text(g,Math.max(0,unit.getInt("distance"))+"м",142,5,MUTED);
                String order=(unit.getBoolean("unloaded")?"Сон · ":"")+unit.getString("order")+(unit.getBoolean("attack")?" · бой":"");
                g.pose().pushPose();g.pose().translate(180,6,0);g.pose().scale(.8f,.8f,1);
                text(g,font.plainSubstrByWidth(order,137),0,0,MUTED);g.pose().popPose();
                if(unit.getBoolean("detect"))g.fill(localWidth-5,6,localWidth-3,12,color(JADE));
            }
            g.pose().popPose();
        }
    }
}
