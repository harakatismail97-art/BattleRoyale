package com.exemple.battleroyale;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {
    private final Paint p = new Paint(3);
    private final Random rng = new Random();
    private final ArrayList<Bot> bots = new ArrayList<>();
    private float px=0, py=0, joyX=0, joyY=0, joyBaseX=0, joyBaseY=0;
    private float screenW, screenH, scale=1;
    private float hp=100, fireCooldown=0;
    private int score=0, ammo=120;
    private long last=System.nanoTime();
    private boolean joy=false, gameOver=false;
    private float zoneRadius=520, worldSize=1800;
    private final ArrayList<Bullet> bullets = new ArrayList<>();

    public GameView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        for(int i=0;i<9;i++) spawnBot();
    }
    private void spawnBot(){
        Bot b=new Bot();
        double a=rng.nextDouble()*Math.PI*2;
        float d=350+rng.nextInt(500);
        b.x=px+(float)Math.cos(a)*d; b.y=py+(float)Math.sin(a)*d;
        b.hp=2; bots.add(b);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        screenW=getWidth(); screenH=getHeight();
        if(px==0 && py==0){px=screenW/2; py=screenH/2;}
        float dt=Math.min(.04f,(System.nanoTime()-last)/1e9f); last=System.nanoTime();
        if(!gameOver) update(dt);
        c.save();
        c.translate(screenW/2-px, screenH/2-py);
        drawWorld(c);
        c.restore();
        drawHud(c);
        if(gameOver) drawGameOver(c);
        postInvalidateDelayed(16);
    }
    private void update(float dt){
        float speed=245;
        px += joyX*speed*dt; py += joyY*speed*dt;
        px=Math.max(50,Math.min(worldSize-50,px)); py=Math.max(50,Math.min(worldSize-50,py));
        zoneRadius=Math.max(180,zoneRadius-2.2f*dt);
        float dist=(float)Math.hypot(px-worldSize/2,py-worldSize/2);
        if(dist>zoneRadius) hp-=8*dt;
        fireCooldown=Math.max(0,fireCooldown-dt);
        for(Bot b:bots){
            float dx=px-b.x, dy=py-b.y, d=(float)Math.hypot(dx,dy);
            if(d>1){b.x+=dx/d*75*dt; b.y+=dy/d*75*dt;}
            if(d<34) hp-=13*dt;
        }
        for(Bullet z:bullets){z.x+=z.vx*dt;z.y+=z.vy*dt;z.life-=dt;}
        for(int i=bullets.size()-1;i>=0;i--){
            Bullet z=bullets.get(i); boolean hit=false;
            for(int j=bots.size()-1;j>=0;j--){
                Bot b=bots.get(j);
                if(Math.hypot(z.x-b.x,z.y-b.y)<23){b.hp--;hit=true;if(b.hp<=0){bots.remove(j);score+=10;}break;}
            }
            if(hit||z.life<=0)bullets.remove(i);
        }
        if(bots.size()<9 && rng.nextFloat()<dt*.45f)spawnBot();
        if(hp<=0){hp=0;gameOver=true;}
    }
    private void drawWorld(Canvas c){
        c.drawColor(Color.rgb(68,116,75));
        p.setColor(Color.rgb(80,130,82));p.setStyle(Paint.Style.FILL);
        for(int x=0;x<worldSize;x+=100)for(int y=0;y<worldSize;y+=100)c.drawRect(x,y,x+96,y+96,p);
        p.setColor(Color.rgb(108,105,88));
        c.drawCircle(worldSize/2,worldSize/2,zoneRadius,p);
        p.setColor(Color.argb(70,30,110,240));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);
        c.drawCircle(worldSize/2,worldSize/2,zoneRadius,p);p.setStyle(Paint.Style.FILL);
        // Cover objects
        for(int i=0;i<18;i++){
            float x=180+(i*197)%1450,y=160+(i*311)%1450;
            p.setColor(Color.rgb(102,75,48));c.drawRoundRect(x-25,y-18,x+25,y+18,7,7,p);
        }
        for(Bullet b:bullets){p.setColor(Color.YELLOW);c.drawCircle(b.x,b.y,6,p);}
        for(Bot b:bots){p.setColor(Color.rgb(170,45,45));c.drawCircle(b.x,b.y,19,p);p.setColor(Color.WHITE);c.drawCircle(b.x-6,b.y-4,3,p);c.drawCircle(b.x+6,b.y-4,3,p);}
        p.setColor(Color.rgb(35,75,180));c.drawCircle(px,py,22,p);
        p.setColor(Color.WHITE);c.drawCircle(px-7,py-6,4,p);c.drawCircle(px+7,py-6,4,p);
        p.setColor(Color.rgb(40,40,40));c.drawRect(px-16,py+19,px+16,py+25,p);
    }
    private void drawHud(Canvas c){
        p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        p.setTextSize(25);p.setColor(Color.WHITE);
        c.drawText("BATTLE ROYALE MINI",24,34,p);
        p.setTextSize(20);c.drawText("HP: "+(int)hp+"    KILLS: "+score/10+"    AMMO: "+ammo,24,64,p);
        p.setColor(Color.DKGRAY);c.drawRoundRect(24,77,224,91,8,8,p);
        p.setColor(Color.GREEN);c.drawRoundRect(24,77,24+200*hp/100f,91,8,8,p);
        // joystick
        p.setColor(Color.argb(90,255,255,255));c.drawCircle(joyBaseX==0?100:joyBaseX,joyBaseY==0?screenH-100:joyBaseY,58,p);
        c.drawCircle(joyBaseX==0?100+joyX*35:joyBaseX+joyX*35,joyBaseY==0?screenH-100+joyY*35:joyBaseY+joyY*35,25,p);
        // fire button
        p.setColor(Color.argb(170,220,55,45));c.drawCircle(screenW-95,screenH-100,54,p);
        p.setColor(Color.WHITE);p.setTextSize(20);c.drawText("FIRE",screenW-121,screenH-93,p);
        p.setTextSize(15);c.drawText("Move: left side  |  Fire: right button",screenW/2-140,screenH-20,p);
    }
    private void drawGameOver(Canvas c){
        p.setColor(Color.argb(190,0,0,0));c.drawRect(0,0,screenW,screenH,p);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(48);
        c.drawText("GAME OVER",screenW/2,screenH/2-10,p);
        p.setTextSize(22);c.drawText("Tap to restart  |  Score: "+score,screenW/2,screenH/2+35,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
    private void shoot(){
        if(fireCooldown>0||ammo<=0||gameOver)return;
        Bot target=null;float best=Float.MAX_VALUE;
        for(Bot b:bots){float d=(float)Math.hypot(b.x-px,b.y-py);if(d<best){best=d;target=b;}}
        if(target!=null&&best<520){
            float dx=target.x-px,dy=target.y-py,d=(float)Math.hypot(dx,dy);
            bullets.add(new Bullet(px,py,dx/d*650,dy/d*650));
        } else bullets.add(new Bullet(px,py,joyX==0?650:joyX*650,joyY==0?-650:joyY*650));
        ammo--;fireCooldown=.24f;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX(),y=e.getY();
        if(gameOver&&e.getAction()==MotionEvent.ACTION_DOWN){reset();return true;}
        switch(e.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(x<screenW*.55f){joy=true;joyBaseX=x;joyBaseY=y;setJoy(x,y);}
                else if(x>screenW-180&&y>screenH-190)shoot();
                return true;
            case MotionEvent.ACTION_MOVE:
                if(joy)setJoy(x,y);
                if(x>screenW-180&&y>screenH-190)shoot();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                joy=false;joyX=joyY=0;joyBaseX=joyBaseY=0;return true;
        }
        return true;
    }
    private void setJoy(float x,float y){
        float dx=x-joyBaseX,dy=y-joyBaseY,d=(float)Math.hypot(dx,dy);
        if(d>55){dx=dx/d*55;dy=dy/d*55;}
        joyX=dx/55;joyY=dy/55;
    }
    private void reset(){
        hp=100;score=0;ammo=120;zoneRadius=520;px=worldSize/2;py=worldSize/2;
        bots.clear();bullets.clear();for(int i=0;i<9;i++)spawnBot();gameOver=false;
    }
    static class Bot{float x,y,hp;}
    static class Bullet{float x,y,vx,vy,life=1.3f;Bullet(float a,float b,float c,float d){x=a;y=b;vx=c;vy=d;}}
}
