package com.example.grabador_sonidos;

public class chorus {
/*	 public Chorus()
     {
         AddSlider(15,1,250,1,"chorus length (ms)");
         AddSlider(1,1,8,1,"number of voices");
         AddSlider(0.5f,0.1f,16,0.1f,"rate (hz)");
         AddSlider(0.7f,0,1,0.1f,"pitch fudge factor");
         AddSlider(-6,-100,12,1,"wet mix (dB)");
         AddSlider(-6,-100,12,1,"dry mix (dB)");
     }*/

   
     int bpos=0;

     float numvoices = 3;
     float choruslen=20;        
     float rateadj = 1.1f;
     float csize = 0.7f;
     int bufofs = 0;
     float wetmix = -6;
     float drymix = -6;
     float[] buffer = new float[1000000];

/*     public override void Slider()
     {
         numvoices=min(16,max(slider2,1));
         choruslen=slider1*SampleRate*0.001f;

         for (int i = 0; i < numvoices; i++)
         {
             buffer[i] = (i + 1) / numvoices * PI;
         }
         
         bufofs=16384;

         csize=choruslen/numvoices * slider4;

         rateadj=slider3*2*PI/SampleRate;
         wetmix = pow(2,slider5/6);
         drymix = pow(2,slider6/6);
     }
     */

     public  void Sample(float spl0, float spl1)
     {
    /*     if(bpos >= choruslen) {
           bpos=0;
         }*/
         float os0=spl0;

         // calculate new sample based on numvoices
         spl0=spl0*drymix;
         float vol=wetmix/numvoices;
         
         for( int i = 0; i < numvoices; i++)
         {
            float tpos = (float) (bpos - (0.5f+0.49f*(Math.sin( Math.PI*(buffer[i] += rateadj))/(Math.PI*buffer[i]))) * (i+1) * csize);

            if(tpos < 0) tpos += choruslen;
            if(tpos > choruslen) tpos -= choruslen;
            float frac=tpos-(int)tpos;
            float tpos2 = (tpos>=choruslen-1) ? 0:tpos+1;
           
            spl0 += (buffer[bufofs+(int)tpos]*(1-frac)+buffer[bufofs+(int)tpos2]*frac) * vol;
         }

         buffer[bufofs+bpos]=os0;
         bpos+=1;

         spl1=spl0;
     }
 }