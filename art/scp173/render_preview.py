"""Render the actual textured cubes for visual QA without changing any artwork."""
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ART = Path(__file__).resolve().parent
MODEL = json.loads((ART/'scp173.bbmodel').read_text(encoding='utf-8'))
TEXTURE = np.array(Image.open(ART/'scp173.png').convert('RGB'))

def rotation(angles):
    x,y,z=np.radians(angles); cx,sx=np.cos(x),np.sin(x); cy,sy=np.cos(y),np.sin(y); cz,sz=np.cos(z),np.sin(z)
    return np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]) @ np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]) @ np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])

def view(yaw, pitch, width=400, height=660):
    canvas=np.zeros((height,width,3),dtype=np.uint8); canvas[:]=[30,35,42]
    zbuf=np.full((height,width),-np.inf)
    t,p=math.radians(yaw),math.radians(pitch)
    right=np.array([math.cos(t),0,math.sin(t)])
    up=np.array([-math.sin(t)*math.sin(p),math.cos(p),math.cos(t)*math.sin(p)])
    camera=np.array([math.sin(t)*math.cos(p),math.sin(p),-math.cos(t)*math.cos(p)])
    basis=np.array([right,up,camera]); scale=15
    light=np.array([-.4,.8,-.6]); light/=np.linalg.norm(light)
    for e in MODEL['elements']:
        x0,y0,z0=e['from']; x1,y1,z1=e['to']
        quads={
            'north':[(x0,y1,z0),(x1,y1,z0),(x1,y0,z0),(x0,y0,z0)],
            'south':[(x1,y1,z1),(x0,y1,z1),(x0,y0,z1),(x1,y0,z1)],
            'east':[(x1,y1,z0),(x1,y1,z1),(x1,y0,z1),(x1,y0,z0)],
            'west':[(x0,y1,z1),(x0,y1,z0),(x0,y0,z0),(x0,y0,z1)],
            'up':[(x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)],
            'down':[(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)]}
        normals={'north':[0,0,-1],'south':[0,0,1],'east':[1,0,0],'west':[-1,0,0],'up':[0,1,0],'down':[0,-1,0]}
        rot=rotation(e['rotation']); pivot=np.array(e['origin'])
        for n,q in quads.items():
            normal=rot @ np.array(normals[n])
            if normal @ camera <=0: continue
            shade=.63+.37*max(0,normal@light)
            vertices=(np.array(q)-pivot) @ rot.T+pivot
            points=vertices @ basis.T
            points[:,0]=width/2+points[:,0]*scale
            points[:,1]=height-50-(points[:,1])*scale
            u,v,u2,v2=e['faces'][n]['uv']; texcoords=np.array([[u,v],[u2,v],[u2,v2],[u,v2]])
            for ids in [[0,1,2],[0,2,3]]:
                pts=points[ids]; uv=texcoords[ids]
                xmin=max(0,int(np.floor(pts[:,0].min()))); xmax=min(width-1,int(np.ceil(pts[:,0].max())))
                ymin=max(0,int(np.floor(pts[:,1].min()))); ymax=min(height-1,int(np.ceil(pts[:,1].max())))
                if xmin>xmax or ymin>ymax: continue
                xx,yy=np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
                a,b,c=pts
                den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
                if abs(den)<1e-9: continue
                w0=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den
                w1=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den; w2=1-w0-w1
                depth=w0*a[2]+w1*b[2]+w2*c[2]
                region=zbuf[ymin:ymax+1,xmin:xmax+1]
                mask=(w0>=-1e-7)&(w1>=-1e-7)&(w2>=-1e-7)&(depth>region)
                uu=np.clip((w0*uv[0,0]+w1*uv[1,0]+w2*uv[2,0]).astype(int),0,127)
                vv=np.clip((w0*uv[0,1]+w1*uv[1,1]+w2*uv[2,1]).astype(int),0,127)
                colors=(TEXTURE[vv,uu]*shade).astype(np.uint8)
                canvas[ymin:ymax+1,xmin:xmax+1][mask]=colors[mask]; region[mask]=depth[mask]
    result=Image.fromarray(canvas); draw=ImageDraw.Draw(result)
    ground=height-50; draw.line([(35,ground+1),(width-35,ground+1)],fill=(90,100,109),width=1)
    return result

if __name__=='__main__':
    image=Image.new('RGB',(1200,720),(30,35,42))
    for i,(yaw,pitch,label) in enumerate([(0,0,'FRONT'),(35,8,'THREE QUARTER'),(90,0,'SIDE')]):
        image.paste(view(yaw,pitch),(400*i,40)); ImageDraw.Draw(image).text((400*i+28,16),label,fill=(211,219,227))
    ImageDraw.Draw(image).text((28,697),'SCP-173  |  36 units / 2.25 blocks  |  128 x 128 texture  |  fixed pose',fill=(158,170,182))
    image.save(ART/'preview.png')
    print('Saved art/scp173/preview.png')
