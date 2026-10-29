"""Reproducible editable Blockbench device sources, runtime exports and previews.

Run with a Python containing Pillow and numpy:
  python tools/build_engineer_assets.py --source "D:/dev_prikols/Модели блокбенч/engineer" --install .
Blockbench itself is not launched. 16 model units = 1 block.
The player uses the normal Minecraft skin; only items and devices are authored here.
"""
from pathlib import Path
import argparse, base64, json, math, shutil, uuid
import numpy as np
from PIL import Image, ImageDraw, ImageFont

NS = uuid.UUID('edca0ca4-3068-4bd6-a1e0-8ec106ec8fce')
PALETTE = ['#202b36', '#384854', '#60747c', '#a4b2af', '#e2dcbd', '#593b29', '#a96636', '#d99b4e', '#f3cb72', '#0c1d24', '#18525c', '#4de0d1', '#a6382d', '#e9763c', '#e9c198', '#392e29']
MODELS = []
def uid(text): return str(uuid.uuid5(NS, text))
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

class Model:
    def __init__(self, name, kind='Item'):
        self.name, self.kind = name, kind
        self.bones, self.cubes, self.clips = {}, [], {}
        MODELS.append(self)
    def bone(self, name, pivot=(0,0,0), parent=None): self.bones[name] = dict(name=name, pivot=list(pivot), parent=parent)
    def cube(self, bone, name, at, size, color):
        assert all(v > 0 and int(v) == v for v in size)
        self.cubes.append(dict(bone=bone, name=name, origin=list(at), size=list(size), color=color))
    def clip(self, name, length, bones, loop=True): self.clips['animation.'+self.name+'.'+name] = dict(loop=loop, animation_length=length, bones=bones)
    def export(self, root, runtime):
        atlas_size = 256 if self.kind == 'Entity' else 128
        atlas = Image.new('RGBA', (atlas_size, atlas_size)); draw = ImageDraw.Draw(atlas)
        x=y=2; row=0
        elements=[]; geometry=[]
        for c in self.cubes:
            w,h,d = c['size']; rw=2*(w+d)+2; rh=h+d+2
            if x+rw >= atlas_size: x=2; y+=row; row=0
            assert y+rh < atlas_size, (self.name, c['name'], y)
            c['uv']=[x,y]; row=max(row,rh)
            color=PALETTE[c['color']]; rgb=tuple(int(color[i:i+2],16) for i in (1,3,5))
            draw.rectangle((x-1,y-1,x+rw-1,y+rh-1), fill=rgb+(255,))
            for yy in range(y,y+rh-1):
                for xx in range(x,x+rw-1):
                    noise=((xx*19+yy*7+c['color']*13)%9)-4
                    draw.point((xx,yy), fill=tuple(max(0,min(255,v+noise)) for v in rgb)+(255,))
            draw.line((x,y,x+rw-2,y), fill=tuple(min(255,v+22) for v in rgb)+(255,))
            u,v=x,y
            faces={'east':[u,v+d,u+d,v+d+h], 'north':[u+d,v+d,u+d+w,v+d+h],
                   'west':[u+d+w,v+d,u+2*d+w,v+d+h], 'south':[u+2*d+w,v+d,u+2*(d+w),v+d+h],
                   'up':[u+d,v,u+d+w,v+d], 'down':[u+d+w,v,u+d+2*w,v+d]}
            c['faces']=faces
            elements.append(dict(uuid=uid(self.name+':cube:'+c['name']),name=c['name'],type='cube',
                **{'from':c['origin'],'to':[a+b for a,b in zip(c['origin'],c['size'])]},
                origin=self.bones[c['bone']]['pivot'],rotation=[0,0,0],box_uv=True,uv_offset=[u,v],
                export=True,visibility=True,faces={side:dict(uv=uv,texture=0) for side,uv in faces.items()}))
            x+=rw
        atlas.save(root/(self.name+'.png'))
        for name,b in self.bones.items():
            geo={k:v for k,v in b.items() if v is not None}
            geo['cubes']=[dict(origin=c['origin'],size=c['size'],uv=c['uv']) for c in self.cubes if c['bone']==name]
            geometry.append(geo)
        def outliner(name):
            return dict(uuid=uid(self.name+':bone:'+name),isOpen=True,children=
                [uid(self.name+':cube:'+c['name']) for c in self.cubes if c['bone']==name]
                +[outliner(n) for n,b in self.bones.items() if b['parent']==name])
        animations=[]
        for fullname,clip in self.clips.items():
            animators={}
            for bone,channels in clip['bones'].items():
                keys=[]
                for channel,track in channels.items():
                    for time,value in track.items():
                        keys.append(dict(uuid=uid(fullname+':'+bone+':'+channel+':'+time),channel=channel,time=float(time),
                            data_points=[dict(zip(('x','y','z'),map(str,value)))],interpolation='linear',color=-1))
                animators[uid(self.name+':bone:'+bone)]=dict(name=bone,type='bone',rotation_global=False,keyframes=keys)
            animations.append(dict(uuid=uid(fullname),name=fullname,length=clip['animation_length'],loop='loop' if clip['loop'] else 'once',snapping=20,animators=animators))
        project=dict(meta=dict(format_version='5.0',model_format='geckolib_model',box_uv=True),name=self.name,
            model_identifier=self.name,geckolib_model_type=self.kind,resolution=dict(width=atlas_size,height=atlas_size),
            elements=elements,groups=[dict(name=n,uuid=uid(self.name+':bone:'+n),origin=b['pivot'],rotation=[0,0,0],export=True,visibility=True,isOpen=True,children=[]) for n,b in self.bones.items()],
            outliner=[outliner(n) for n,b in self.bones.items() if b['parent'] is None],
            textures=[dict(uuid=uid(self.name+':texture'),name=self.name+'.png',id='0',width=atlas_size,height=atlas_size,
                uv_width=atlas_size,uv_height=atlas_size,source='data:image/png;base64,'+base64.b64encode((root/(self.name+'.png')).read_bytes()).decode(),
                relative_path=self.name+'.png',internal=True,saved=True,visible=True,render_mode='default',render_sides='auto')],animations=animations)
        write(root/(self.name+'.bbmodel'),project)
        geo=dict(format_version='1.12.0',**{'minecraft:geometry':[dict(description=dict(identifier='geometry.'+self.name,texture_width=atlas_size,texture_height=atlas_size,visible_bounds_width=3,visible_bounds_height=3,visible_bounds_offset=[0,1,0]),bones=geometry)]})
        write(root/(self.name+'.geo.json'),geo); write(root/(self.name+'.animation.json'),dict(format_version='1.8.0',animations=self.clips))
        for folder,ext in [('geo','.geo.json'),('animations','.animation.json')]: write(runtime/folder/(self.name+ext), geo if folder=='geo' else dict(format_version='1.8.0',animations=self.clips))
        texture_folder = 'entity' if self.kind=='Entity' else 'block' if self.kind=='Block' else 'item'
        dest=runtime/'textures'/texture_folder/(self.name+'.png'); dest.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(root/(self.name+'.png'),dest)
        if self.kind!='Entity':
            item=dict(parent='minecraft:block/block',textures={'0':'maniacrev:'+texture_folder+'/'+self.name,'particle':'maniacrev:'+texture_folder+'/'+self.name},elements=[],display={
                'gui':dict(rotation=[25,215,0],translation=[0,0,0],scale=[.8,.8,.8]),
                'firstperson_righthand':dict(rotation=[-10,175,-5],translation=[1,2,0],scale=[.65,.65,.65]),
                'firstperson_lefthand':dict(rotation=[-10,185,5],translation=[1,2,0],scale=[.65,.65,.65]),
                'thirdperson_righthand':dict(rotation=[0,-90,0],translation=[0,2,0],scale=[.55,.55,.55]),
                'thirdperson_lefthand':dict(rotation=[0,90,0],translation=[0,2,0],scale=[.55,.55,.55]),
                'ground':dict(rotation=[0,0,0],translation=[0,3,0],scale=[.4,.4,.4])})
            if self.name=='engineer_barricade': item['display']['gui']=dict(rotation=[20,215,0],translation=[0,-4,0],scale=[.55,.55,.55])
            for c in self.cubes:
                at=[c['origin'][0]+8,c['origin'][1],c['origin'][2]+8]
                item['elements'].append({'from':at,'to':[a+b for a,b in zip(at,c['size'])], 'faces':{s:dict(uv=[v*16/atlas_size for v in uv],texture='#0') for s,uv in c['faces'].items()}})
            if self.name=='engineer_tablet':
                item['textures']['screen']='maniacrev:item/engineer_tablet_screen'
                item['elements'][next(i for i,c in enumerate(self.cubes) if c['name']=='glass')]['faces']['north']=dict(uv=[0,0,16,16],texture='#screen')
                item['display']['firstperson_righthand']=dict(rotation=[-10,175,0],translation=[-2,5,0],scale=[.5,.5,.5])
                item['display']['firstperson_lefthand']=dict(rotation=[-10,185,0],translation=[-2,5,0],scale=[.5,.5,.5])
            write(root/(self.name+'.item.json'),item); write(runtime/'models/item'/(self.name+'.json'),item)
        validate(project,geometry,self.clips,atlas_size)

def track(points, channel='rotation'): return {channel:{str(t):v for t,v in points}}
def constant(value, length=1, channel='rotation'): return track([(0,value),(length,value)],channel)
def devices():
    m=Model('engineer_barricade','Block'); m.bone('root')
    for x in (-8,6):
        m.cube('root','post_'+str(x),(x,0,-2),(2,24,4),2)
        m.cube('root','foot_'+str(x),(x,0,-4),(2,2,8),0)
        m.cube('root','cap_'+str(x),(x,23,-2),(2,1,4),7)
    for y in (4,13,21): m.cube('root','rail_'+str(y),(-6,y,-1),(12,2,2),6)
    for x in (-4,0,4): m.cube('root','lattice_'+str(x),(x,6,0),(1,15,1),3)
    m.cube('root','warning_plate',(-5,8,-2),(10,4,1),7)
    for x in (-4,0,4): m.cube('root','warning_mark_'+str(x),(x,8,-3),(1,4,1),0)
    m.clip('idle',1,{},True)
    m=Model('engineer_panel','Block'); m.bone('root'); m.bone('speaker',(0,6,-4),'root'); m.bone('antenna',(4,11,1),'root')
    m.cube('root','cabinet',(-5,0,-4),(10,12,8),0)
    m.cube('root','frame_top',(-5,11,-5),(10,1,1),7)
    m.cube('root','frame_bottom',(-5,0,-5),(10,1,1),7)
    m.cube('root','frame_left',(-5,1,-5),(1,10,1),6)
    m.cube('root','frame_right',(4,1,-5),(1,10,1),6)
    m.cube('speaker','speaker_rim',(-4,2,-5),(8,8,1),2)
    m.cube('speaker','speaker_cone',(-3,3,-6),(6,6,1),9)
    m.cube('speaker','speaker_hub',(-1,5,-7),(2,2,1),6)
    for x in (-3,0,3): m.cube('root','charge_light_'+str(x),(x,10,-6),(1,1,1),11)
    m.cube('antenna','antenna_rod',(4,11,1),(1,7,1),3)
    m.cube('antenna','antenna_tip',(4,17,1),(1,1,1),13)
    m.cube('root','handle',(-3,12,-1),(6,1,2),5)
    m.clip('idle',1,{},True)
    m.clip('play',1,{'antenna':track([(0,[0,0,-4]),(.5,[0,0,4]),(1,[0,0,-4])])})
    m=Model('engineer_tablet'); m.bone('root')
    m.cube('root','chassis',(-6,2,-1),(12,14,2),0)
    m.cube('root','copper_border',(-5,3,-2),(10,12,1),6)
    m.cube('root','glass',(-4,5,-3),(8,9,1),10)
    m.cube('root','status_light',(-3,4,-3),(1,1,1),11)
    m.cube('root','dial',(3,3,-3),(2,2,1),7)
    m.cube('root','top_grip',(-4,16,-1),(8,1,2),5)
    m.clip('idle',1,{},True)
    m=Model('engineer_remote'); m.bone('root'); m.bone('button',(0,6,-2),'root')
    m.cube('root','case',(-3,1,-2),(6,10,4),0)
    m.cube('root','side_grip_left',(-4,2,-1),(1,7,2),6)
    m.cube('root','side_grip_right',(3,2,-1),(1,7,2),6)
    m.cube('root','display',(-2,7,-3),(4,3,1),10)
    m.cube('root','signal',(-1,8,-4),(2,1,1),11)
    m.cube('button','button',(-1,4,-3),(2,2,1),13)
    m.cube('root','antenna',(1,11,0),(1,6,1),3)
    m.cube('root','antenna_cap',(1,16,0),(1,1,1),7)
    m.clip('press',.3,{'button':track([(0,[0,0,0]),(.1,[0,0,1]),(.2,[0,0,1]),(.3,[0,0,0])],'position')},False)
    m=Model('engineer_scrap'); m.bone('root')
    # Steel threaded bolt, a smaller brass bolt and a loose nut with an actual hole.
    m.cube('root','steel_shaft',(-1,2,-3),(2,9,2),3)
    for y in (3,5,7,9): m.cube('root','steel_thread_'+str(y),(-2,y,-4),(4,1,4),2)
    m.cube('root','steel_head_x',(-3,11,-4),(6,3,4),3)
    m.cube('root','steel_head_z',(-2,11,-5),(4,3,6),3)
    m.cube('root','head_slot',(-.5,14,-3.5),(1,1,3),0)
    m.cube('root','brass_shaft',(4,1,1),(2,7,2),6)
    for y in (2,4,6): m.cube('root','brass_thread_'+str(y),(3,y,0),(4,1,4),7)
    m.cube('root','brass_head',(3,8,0),(4,2,4),8)
    m.cube('root','nut_left',(-7,1,-1),(2,2,6),3)
    m.cube('root','nut_right',(-3,1,-1),(2,2,6),3)
    m.cube('root','nut_front',(-5,1,-1),(2,2,2),2)
    m.cube('root','nut_back',(-5,1,3),(2,2,2),3)
    m.clip('idle',1,{},True)

def validate(project,bones,clips,size):
    ids=[e['uuid'] for e in project['elements']]+[b['uuid'] for b in project['groups']]
    assert len(ids)==len(set(ids)); known=set(ids)
    def check(nodes):
        for node in nodes:
            assert (node if isinstance(node,str) else node['uuid']) in known
            if isinstance(node,dict): check(node['children'])
    check(project['outliner']); names={b['name'] for b in bones}
    for b in bones:
        assert b.get('parent') is None or b['parent'] in names
        for c in b['cubes']:
            assert all(math.isfinite(v) for v in c['origin']+c['size'])
            assert c['uv'][0]+2*(c['size'][0]+c['size'][2]) <= size
            assert c['uv'][1]+c['size'][1]+c['size'][2] <= size
    for clip in clips.values():
        ids=[]
        for bone,channels in clip['bones'].items():
            assert bone in names
            for channel,values in channels.items():
                for time,value in values.items():
                    assert 0<=float(time)<=clip['animation_length']
                    assert len(value)==3 and all(math.isfinite(v) for v in value)
    for anim in project['animations']:
        key_ids=[k['uuid'] for a in anim['animators'].values() for k in a['keyframes']]
        assert len(key_ids)==len(set(key_ids))

def matrix(rotation):
    x,y,z=map(math.radians,rotation); cx,sx=math.cos(x),math.sin(x); cy,sy=math.cos(y),math.sin(y); cz,sz=math.cos(z),math.sin(z)
    return np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
def sample(track,time):
    values=sorted((float(t),np.array(v,dtype=float)) for t,v in track.items())
    if time<=values[0][0]: return values[0][1]
    for (a,va),(b,vb) in zip(values,values[1:]):
        if a<=time<=b: return va+(vb-va)*(time-a)/(b-a)
    return values[-1][1]
def preview(model,clip='idle',time=0,yaw=-25,side=260):
    image=Image.new('RGBA',(side,side),(17,24,31,255)); d=ImageDraw.Draw(image)
    animation=model.clips.get('animation.'+model.name+'.'+clip,dict(bones={}))
    transforms={}
    def transform(name):
        if name in transforms:return transforms[name]
        info=model.bones[name]; pivot=np.array(info['pivot'],float); channels=animation['bones'].get(name,{})
        rot=matrix(sample(channels['rotation'],time) if 'rotation' in channels else [0,0,0])
        pos=sample(channels['position'],time) if 'position' in channels else np.zeros(3)
        mat=np.eye(4); mat[:3,:3]=rot; mat[:3,3]=pivot-rot@pivot+pos
        if info['parent']: mat=transform(info['parent'])@mat
        transforms[name]=mat; return mat
    view=matrix([12,yaw,0]); faces=[]
    corners=np.array([(x,y,z,1) for x,y,z in [(0,0,0),(1,0,0),(1,1,0),(0,1,0),(0,0,1),(1,0,1),(1,1,1),(0,1,1)]])
    for c in model.cubes:
        pts=corners.copy().astype(float); pts[:,:3]=pts[:,:3]*np.array(c['size'])+np.array(c['origin'])
        pts=(transform(c['bone'])@pts.T).T[:,:3]@view.T
        for indices,shade in [([0,1,2,3],1),([4,7,6,5],.65),([0,4,5,1],.55),([3,2,6,7],1.2),([1,5,6,2],.8),([0,3,7,4],.9)]:
            p=pts[indices]; a,b,cc=p[:3]; normal=np.cross(b-a,cc-b)
            if normal[2]>=0:continue
            color=PALETTE[c['color']]; rgb=tuple(min(255,int(int(color[i:i+2],16)*shade)) for i in (1,3,5))
            faces.append((float(p[:,2].mean()),p,rgb))
    allpts=np.concatenate([f[1] for f in faces]); lo=allpts[:,:2].min(axis=0); hi=allpts[:,:2].max(axis=0)
    scale=min((side-42)/max(1,hi[0]-lo[0]),(side-36)/max(1,hi[1]-lo[1])); mid=(lo+hi)/2
    for _,p,color in sorted(faces,key=lambda f:-f[0]):
        xy=[(side/2+(q[0]-mid[0])*scale,side/2-(q[1]-mid[1])*scale) for q in p]
        d.polygon(xy,fill=color+(255,),outline=tuple(max(0,v-28) for v in color)+(255,))
    return image

def extras(root,runtime):
    screen=Image.new('RGBA',(16,128)); d=ImageDraw.Draw(screen)
    for frame in range(8):
        y=frame*16; d.rectangle((0,y,15,y+15),fill='#0c1d24'); d.rectangle((1,y+1,14,y+3),fill='#18525c')
        for row in range(3):
            d.rectangle((2,y+5+row*3,3,y+6+row*3),fill='#d99b4e'); d.line((5,y+5+row*3,8+(frame+row)%5,y+5+row*3),fill='#4de0d1')
        d.line((1,y+frame*2,14,y+frame*2),fill='#75e9d6')
    screen.save(root/'engineer_tablet_screen.png'); write(root/'engineer_tablet_screen.png.mcmeta',{'animation':{'frametime':3,'width':16,'height':16,'interpolate':False}})
    for file in ['engineer_tablet_screen.png','engineer_tablet_screen.png.mcmeta']: shutil.copy2(root/file,runtime/'textures/item'/file)
    # Source model contains the animated screen as a portable secondary texture as well.
    path=root/'engineer_tablet.bbmodel'; project=json.loads(path.read_text(encoding='utf-8'))
    project['textures'].append(dict(uuid=uid('engineer:screen'),name='engineer_tablet_screen.png',id='1',width=16,height=128,uv_width=16,uv_height=16,
        source='data:image/png;base64,'+base64.b64encode((root/'engineer_tablet_screen.png').read_bytes()).decode(),internal=True,frame_time=3,frame_order_type='loop'))
    next(e for e in project['elements'] if e['name']=='glass')['faces']['north']=dict(uv=[0,0,16,16],texture=1)
    write(path,project)
    sheet=Image.new('RGBA',(520,math.ceil(len(MODELS)/2)*260),(17,24,31,255)); draw=ImageDraw.Draw(sheet)
    for n,model in enumerate(MODELS):
        x=n%2*260; y=n//2*260
        sheet.alpha_composite(preview(model,side=260),(x,y))
        draw.text((x+10,y+8),model.name.replace('engineer_','').upper(),fill='#d99b4e')
    sheet.save(root/'engineer_preview.png')
    scrap=next(model for model in MODELS if model.name=='engineer_scrap')
    scrap_preview=Image.new('RGBA',(780,260),(17,24,31,255))
    for i,yaw in enumerate((0,90,-25)): scrap_preview.alpha_composite(preview(scrap,yaw=yaw,side=260),(i*260,0))
    scrap_preview.save(root/'engineer_scrap_preview.png')
    card=sheet.crop((0,0,520,520)).resize((512,512)).convert('RGB'); card.save(root/'engineer_card.png')
    target=runtime/'textures/gui/frescos/engineer.png'; target.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(root/'engineer_card.png',target)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--source',type=Path,required=True); parser.add_argument('--install',type=Path,required=True); args=parser.parse_args()
    root=args.source.resolve(); runtime=args.install.resolve()/'src/main/resources/assets/maniacrev'; root.mkdir(parents=True,exist_ok=True)
    devices()
    for model in MODELS: model.export(root,runtime)
    extras(root,runtime)
    shutil.copy2(Path(__file__),root/'build_engineer_assets.py')
    print(json.dumps(dict(models=len(MODELS),cubes=sum(len(m.cubes) for m in MODELS),source=str(root),preview=str(root/'engineer_preview.png')),ensure_ascii=False))
if __name__=='__main__': main()
