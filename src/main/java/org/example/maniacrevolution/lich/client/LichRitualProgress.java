package org.example.maniacrevolution.lich.client;

import net.minecraft.nbt.CompoundTag;

/** Server-clock presentation only. Never completes a ritual or spends resources locally. */
public final class LichRitualProgress {
    public record Frame(String state,String action,int kind,int entity,float progress,boolean visible) {
        public boolean failed() {return state.equals("cancelled")||state.equals("failed");}
        public boolean casting() {return state.equals("casting");}
        public boolean showAtReticle() {return visible&&!state.equals("emerge")&&!state.equals("ready");}
    }
    private String state="",action="";
    private long serial=-1,start,finish;
    private int kind,entity=-1,focus=-1;
    public void clear() {state="";serial=-1;entity=focus=-1;}
    public void accept(CompoundTag tag) {
        String incoming=tag.getString("state");long next=tag.getLong("serial");
        if(!java.util.Set.of("casting","emerge","complete","cancelled","failed").contains(incoming)||next<serial)return;
        if(next==serial&&!state.isEmpty()&&!state.equals("casting")&&incoming.equals("casting"))return;
        serial=next;state=incoming;action=tag.getString("action");start=tag.getLong("start");finish=Math.max(start+1,tag.getLong("finish"));
        kind=Math.max(0,Math.min(3,tag.getInt("kind")));entity=tag.getInt("entity");
        focus=tag.contains("focus")?tag.getInt("focus"):-1;
    }
    public void lostEmergingUnit(long now,boolean killed) {
        if(!state.equals("emerge"))return;
        if(killed) {state="failed";action="emerge";start=now;finish=now+16;}
        else clear();
    }
    public Frame frame(double now) {
        if(state.isEmpty())return new Frame("","",0,-1,0,false);
        boolean running=state.equals("casting")||state.equals("emerge");
        float progress=(float)Math.max(0,Math.min(state.equals("casting")?.99:1,(now-start)/(finish-start)));
        boolean visible=!state.isEmpty()&&now<=finish+(running?16:0);
        String shown=state.equals("emerge")&&now>=finish?"ready":state;
        return new Frame(shown,action,kind,entity,running?progress:state.equals("complete")?1:0,visible);
    }
    public long start() {return start;}
    int focus() {return focus;}
}
