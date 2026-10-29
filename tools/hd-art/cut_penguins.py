"""Cut the purple and green penguins out of the HD penguin sprite sheets
(first animation frame), for the app icon (see hd_icon.py).

Usage: python3 cut_penguins.py <res-dir> <out-dir>
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image
RES = os.path.join(sys.argv[1], 'drawable-nodpi')
S = sys.argv[2]

def finish(img, mask):
    bgd=np.zeros((1,65),np.float64); fgd=np.zeros((1,65),np.float64)
    cv2.grabCut(img,mask,None,bgd,fgd,10,cv2.GC_INIT_WITH_MASK)
    m=np.where((mask==1)|(mask==3),255,0).astype(np.uint8)
    n,lab,stats,_=cv2.connectedComponentsWithStats(m)
    k=1+np.argmax(stats[1:,cv2.CC_STAT_AREA]); m=np.where(lab==k,255,0).astype(np.uint8)
    inv=255-m; n2,lab2,st2,_=cv2.connectedComponentsWithStats(inv)
    for i in range(1,n2):
        x,y,w,h,a=st2[i]
        if x>0 and y>0 and x+w<m.shape[1] and y+h<m.shape[0]: m[lab2==i]=255
    m=cv2.morphologyEx(m,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    m=cv2.GaussianBlur(m,(3,3),0)
    rgba=cv2.cvtColor(img,cv2.COLOR_BGR2RGBA); rgba[:,:,3]=m
    return Image.fromarray(rgba)

# Green penguin (faces right; launcher handle on the right).
img=cv2.imread(os.path.join(RES, 'penguins2_hd.jpg'))[0:180,0:228].copy()
b,g,r=[img[:,:,i].astype(int) for i in range(3)]
mask=np.full(img.shape[:2],cv2.GC_PR_BGD,np.uint8)
mask[15:180,20:156]=cv2.GC_PR_FGD
mask[(b-g>12)]=cv2.GC_BGD
mask[(g-b>25)&(g-r>15)]=cv2.GC_FGD
mask[88:180,156:228]=cv2.GC_BGD      # flipper + handle + background
mask[:,176:228]=cv2.GC_BGD
mask[100:180,0:30]=cv2.GC_BGD        # snow
finish(img,mask).save(S+'/cut_green.png')

# Purple penguin (faces left; launcher handle on the left).
img=cv2.imread(os.path.join(RES, 'penguins_hd.jpg'))[0:180,0:228].copy()
b,g,r=[img[:,:,i].astype(int) for i in range(3)]
mask=np.full(img.shape[:2],cv2.GC_PR_BGD,np.uint8)
mask[12:180,62:228]=cv2.GC_PR_FGD
mask[0:60,0:62]=cv2.GC_BGD
mask[60:132,0:72]=cv2.GC_BGD         # handle
mask[100:150,140:200]=cv2.GC_FGD     # body
finish(img,mask).save(S+'/cut_purple.png')
