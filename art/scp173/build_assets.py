"""Rebuild the SCP-173 Blockbench project and GeckoLib 4 assets (Python/Pillow)."""
import base64
import json
from pathlib import Path
import shutil
import uuid
from PIL import Image

ART = Path(__file__).resolve().parent
ROOT = ART.parents[1]
RES = ROOT / 'src/main/resources/assets/maniacrev'
MODEL_SCALE = 36 / 32  # 36 model units = 2.25 blocks; scale about the feet.
VISIBLE_BOX = [1.5 * MODEL_SCALE, 2.5 * MODEL_SCALE, MODEL_SCALE]

def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'maniacrev/scp173/' + name))

def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def build():
    # Normalize generated artwork for a pixel texture; do not repaint the reference.
    Image.open(ART / 'texture_source.png').convert('RGBA').resize(
        (128, 128), Image.Resampling.BOX).save(ART / 'scp173.png')
    (RES / 'textures/entity').mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ART / 'scp173.png', RES / 'textures/entity/scp173.png')
    groups, elements, by_name = [], [], {}
    def group(name, parent, pivot, color):
        g = dict(name=name, uuid=uid(name), origin=pivot, rotation=[0,0,0], color=color,
                 export=True, isOpen=True, visibility=True, children=[])
        groups.append((g,parent)); by_name[name] = g
        if parent: by_name[parent]['children'].append(g)
    def cube(name, bone, lo, hi, pivot=None, rotation=None, face=False):
        i = len(elements); s = [hi[j]-lo[j] for j in range(3)]; faces = {}
        for n,(a,b) in dict(north=(0,1),south=(0,1),east=(2,1),west=(2,1),up=(0,2),down=(0,2)).items():
            w,h = max(1,round(s[a]*4)),max(1,round(s[b]*4))
            u = 66+(i*7+len(n)*3)%max(1,60-w)
            v = 2+(i*13+len(n)*11)%max(1,124-h)
            rect = [u,v,u+w,v+h]
            if face and n == 'north':
                rect = [round((lo[0]+3.7)/7.4*64,4),round((32-hi[1])/10.5*128,4),
                        round((hi[0]+3.7)/7.4*64,4),round((32-lo[1])/10.5*128,4)]
            faces[n] = dict(uv=rect,texture=0)
        e = dict(name=name,type='cube',uuid=uid(name),origin=pivot or by_name[bone]['origin'],
                 rotation=rotation or [0,0,0],box_uv=False,autouv=0,color=by_name[bone]['color'],
                 export=True,visibility=True,shade=True,faces=faces)
        e.update({'from':lo,'to':hi}); elements.append(e)
        by_name[bone]['children'].append(e['uuid'])
    for args in [('root',None,[0,0,0],0),('body','root',[0,12,0],1),('head','body',[0,21.5,0],2),
                 ('left_arm','body',[2.2,20.2,0],3),('left_forearm','left_arm',[4.1,18.5,-.4],3),
                 ('right_arm','body',[-2.2,20.2,0],4),('right_forearm','right_arm',[-4.1,18.5,-.4],4),
                 ('left_leg','root',[1.25,12,.3],5),('right_leg','root',[-1.25,12,.3],6)]: group(*args)
    for name,lo,hi in [('pelvis',[-2.25,10.8,-1.6],[2.25,14,2.15]),
                       ('torso_lower',[-2,14,-1.5],[2,18.5,1.9]),
                       ('shoulders',[-2.35,18.5,-1.5],[2.35,20.8,1.8]),
                       ('neck',[-1.4,20.8,-1.25],[1.4,22.2,1.3]),
                       ('back_haunch',[-1.85,11.8,1.8],[1.85,15.3,2.8])]: cube(name,'body',lo,hi)
    for name,y0,y1,w,f,b in [('head_chin',21.5,23,3.8,-2.35,1.65),
                            ('head_lower',23,25,5.8,-2.8,2.25),('head_main',25,29.5,7.4,-3,2.6),
                            ('head_upper',29.5,31,6.6,-2.8,2.35),('head_crown',31,32,4.6,-2.25,1.85)]:
        cube(name,'head',[-w/2,y0,f],[w/2,y1,b],face=True)
    # Rest pose is encoded in cube rotations; bones are available for future animation.
    for side,sign in [('left',1),('right',-1)]:
        x=2.2*sign
        cube(side+'_upper_arm',side+'_arm',[x-.8,17.3,-.8],[x+.8,20.3,.8],
             [x,20.2,0],[-8,0,sign*48])
        x=4.1*sign
        cube(side+'_forearm_stone',side+'_forearm',[x-.8,17.8,-3.1],[x+.8,19.4,-.25],
             [x,18.5,-.4],[-22,sign*10,0])
        cube(side+'_hand',side+'_forearm',[x-.9,18.65,-3.6],[x+.9,20.15,-2.1],
             [x,19.3,-2.7],[0,sign*10,0])
        x=1.25*sign
        cube(side+'_thigh',side+'_leg',[x-1,6,-.95],[x+1,12,1.15])
        cube(side+'_shin',side+'_leg',[x-.85,1.2,-.8],[x+.85,6,1])
        cube(side+'_foot',side+'_leg',[x-.95,0,-1.25],[x+.95,1.2,1.1])
    for g, _ in groups:
        g['origin'] = [round(v * MODEL_SCALE, 6) for v in g['origin']]
    for e in elements:
        for key in ('from', 'to', 'origin'):
            e[key] = [round(v * MODEL_SCALE, 6) for v in e[key]]
    png=(ART/'scp173.png').read_bytes()
    project=dict(meta=dict(format_version='4.10',model_format='bedrock',box_uv=False),name='scp173',
                 model_identifier='scp173',visible_box=VISIBLE_BOX,resolution=dict(width=128,height=128),
                 elements=elements,outliner=[by_name['root']],animations=[],textures=[dict(
                 name='scp173.png',uuid=uid('texture'),id='0',namespace='maniacrev',folder='entity',
                 width=128,height=128,uv_width=128,uv_height=128,mode='bitmap',relative_path='scp173.png',
                 internal=True,source='data:image/png;base64,'+base64.b64encode(png).decode())])
    save(ART/'scp173.bbmodel',project)
    element_map={e['uuid']:e for e in elements}; bones=[]
    # Same coordinate and face UV conversion as Blockbench's Bedrock codec.
    for g,parent in groups:
        bone=dict(name=g['name'],pivot=[-g['origin'][0],*g['origin'][1:]])
        if parent: bone['parent']=parent
        cubes=[]
        for child in g['children']:
            if not isinstance(child,str): continue
            e=element_map[child]
            c=dict(origin=[-e['to'][0],*e['from'][1:]],
                   size=[round(e['to'][i]-e['from'][i],4) for i in range(3)],uv={})
            if any(e['rotation']):
                c['pivot']=[-e['origin'][0],*e['origin'][1:]]
                c['rotation']=[-e['rotation'][0],-e['rotation'][1],e['rotation'][2]]
            for n,face in e['faces'].items():
                u,v,u2,v2=face['uv']
                c['uv'][n]=dict(uv=[u,v],uv_size=[round(u2-u,4),round(v2-v,4)])
                if n in ('up','down'):
                    c['uv'][n]=dict(uv=[u2,v2],uv_size=[round(u-u2,4),round(v-v2,4)])
            cubes.append(c)
        if cubes: bone['cubes']=cubes
        bones.append(bone)
    save(RES/'geo/scp173.geo.json',{'format_version':'1.12.0','minecraft:geometry':[dict(
        description=dict(identifier='geometry.scp173',texture_width=128,texture_height=128,
                         visible_bounds_width=VISIBLE_BOX[0],visible_bounds_height=VISIBLE_BOX[1],
                         visible_bounds_offset=[0,VISIBLE_BOX[2],0]),
        bones=bones)]})
    # GeoModel requires an animation resource path; there are deliberately no clips.
    save(RES/'animations/scp173.animation.json',dict(format_version='1.8.0',animations={}))
    print(f'Built {len(elements)} cubes, {len(bones)} bones, 128x128 texture')

if __name__=='__main__': build()
