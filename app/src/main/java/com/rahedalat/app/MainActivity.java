package com.rahedalat.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayDeque;

public class MainActivity extends Activity {
    private GameView game;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(12,16,20));
        getWindow().setNavigationBarColor(Color.rgb(12,16,20));
        game = new GameView(this);
        setContentView(game);
    }

    @Override public void onBackPressed() {
        if (!game.goBack()) game.askExit();
    }

    public static class GameView extends View {
        static final int START=0, OFFICE=1, LIBRARY=2, CASE=3, LESSON=4, QUIZ=5,
                RESULT=6, PROGRESS=7, ARCHIVE=8, LAWBOOK=9,
                CASE_ROOM=10, CASE1_AMIR=11, CASE1_EVIDENCE=13, CASE1_PHONE=14, CASE1_RECEIPTS=15, CASE1_DVD=16;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final ArrayDeque<Integer> history = new ArrayDeque<>();
        final SharedPreferences prefs;

        Bitmap officeBg, libraryBg, lawyer, client, judge, officer;
        Bitmap caseRoomAmir, amirCloseup, amirEvidenceDialogue, evidenceOpen;
        Bitmap phoneMessagesIcon, phoneReceiptIcon;
        final Bitmap[] receiptPieces = new Bitmap[22];
        final float[] receiptPieceX = new float[22];
        final float[] receiptPieceY = new float[22];
        final float[] receiptTargetX = new float[22];
        final float[] receiptTargetY = new float[22];
        final float[] receiptRot = new float[22];
        final boolean[] receiptOnBoard = new boolean[22];
        final boolean[] receiptLocked = new boolean[22];
        final float[] receiptTrayX = new float[22];
        final float[] receiptTrayY = new float[22];
        int selectedReceiptPiece = -1;
        String phoneInput = "";
        int phonePage = 0; // 0=home, 1=messages, 2=transfer receipt
        boolean phoneSolved = false, receiptSolved = false, dvdSolved = false;
        MediaPlayer music;
        int screen = START;
        int selectedCase = 1;
        int storyStep = 0;
        boolean settings = false, exitConfirm = false, answerChosen = false, correct = false;
        float ox, oy, scale;
        float roomPan = 0f, roomDownX = 0f, roomStartPan = 0f;
        boolean roomDragging = false;
        int case1Dialogue = 0;

        final String[] titles = {
                "",
                "پرونده قرارداد و امضای مورد اختلاف",
                "پرونده مطالبه وجه و تشخیص طرفین دعوا",
                "پرونده نقص دادخواست و تکمیل اطلاعات",
                "پرونده مطالبه خسارت و تعیین خواسته",
                "پرونده سند و اثبات ادعا"
        };

        final String[] casePeople = {"", "مریم احمدی", "رضا کریمی", "سارا موسوی", "علی نادری", "نگار حسینی"};
        final String[] caseIssues = {
                "",
                "امضای قراردادی که موکل انتساب آن را نمی‌پذیرد.",
                "مطالبه وجهی که در آن باید خواهان و خوانده درست شناسایی شوند.",
                "دادخواستی که بخشی از اطلاعات ضروری آن ناقص ثبت شده است.",
                "مطالبه خسارت و تشخیص دقیق چیزی که از دادگاه درخواست می‌شود.",
                "اختلاف درباره سند و ارزش آن به عنوان دلیل در دعوا."
        };

        final String[][] story = {
                {},
                {"من قراردادی را امضا نکرده‌ام، اما طرف مقابل نسخه‌ای از قرارداد را ارائه کرده است.",
                 "اول باید بدانیم دعوا از چه مسیری وارد دادگاه شده و چه چیزی از دادگاه خواسته شده است.",
                 "اگر امضا را قبول ندارم، باید ادعای خود را روشن و دلیل‌های موجود را بررسی کنم.",
                 "حالا پرونده آماده است تا ببینیم نخستین اقدام حقوقی درست چیست."},
                {"من از شخصی طلب دارم و می‌خواهم مبلغ بدهی را از راه قانونی مطالبه کنم.",
                 "قبل از هر چیز باید مشخص کنیم چه کسی دعوا را مطرح می‌کند و دعوا علیه چه کسی است.",
                 "پس در این پرونده، عنوان خواهان و خوانده فقط یک اسم نیست؛ نقش هر طرف را مشخص می‌کند.",
                 "حالا باید تصمیم بگیریم در دادخواست چه کسی خواهان و چه کسی خوانده است."},
                {"دادخواست ثبت شده، اما بخشی از اطلاعات آن ناقص است و دفتر دادگاه ایراد گرفته است.",
                 "برای ادامه رسیدگی، باید بدانیم دادخواست چه اطلاعاتی لازم دارد و هر بخش چه کاربردی دارد.",
                 "مشخصات طرفین، خواسته و دلایل از اطلاعاتی هستند که باید با دقت بررسی شوند.",
                 "حالا نوبت توست که تشخیص بدهی کدام اطلاعات برای رفع ایراد مهم‌تر است."},
                {"من خسارتی دیده‌ام و می‌خواهم جبران آن را از دادگاه بخواهم.",
                 "خواسته باید روشن باشد؛ دادگاه باید بداند دقیقاً چه چیزی از او درخواست شده است.",
                 "همچنین در موارد لازم، ارزش خواسته می‌تواند در هزینه دادرسی اثر داشته باشد.",
                 "پس قبل از ادامه پرونده باید خواسته را دقیق و قابل فهم بیان کنیم."},
                {"طرف مقابل می‌گوید سندی دارد که ادعای او را ثابت می‌کند، اما من درباره آن اختلاف دارم.",
                 "اینجا باید مفهوم دلیل را بشناسیم؛ هر ادعا برای اثبات خود به دلیل مناسب نیاز دارد.",
                 "سند یکی از ادله است، اما باید جایگاه و ارتباط آن با ادعا را بررسی کرد.",
                 "اکنون باید تشخیص بدهی دلیل در این پرونده چه نقشی دارد."}
        };

        final String[][] lesson = {
                {},
                {"آموزش پرونده امیر: دادخواست وسیله طرح بسیاری از دعاوی مدنی است. پس از جمع‌آوری و بررسی دلایل، باید خواسته و مشخصات طرفین را روشن و دادخواست را مطابق مقررات به مرجع صالح ارائه کرد.",
                 "نکته آموزشی: دادخواست — نوشته‌ای که خواهان برای طرح دعوا و آغاز رسیدگی، مطابق مقررات به مرجع صالح ارائه می‌کند."},
                {"خواهان کسی است که دعوا را مطرح می‌کند و خوانده کسی است که دعوا علیه او مطرح شده است. تشخیص این دو نقش برای تنظیم درست پرونده ضروری است.",
                 "اصطلاح کلیدی: خواهان و خوانده — دو عنوان ناظر به نقش طرفین در دعوا، نه صرفاً نام اشخاص."},
                {"دادخواست باید اطلاعات لازم درباره اصحاب دعوا، خواسته و جهات و دلایل مورد استناد را در حدود قانون داشته باشد. نقص اطلاعات می‌تواند روند پرونده را متوقف یا اصلاح‌پذیر کند.",
                 "اصطلاح کلیدی: مشخصات اصحاب دعوا — اطلاعاتی که برای شناسایی و ابلاغ به طرفین لازم است."},
                {"خواسته همان چیزی است که خواهان از دادگاه می‌خواهد. روشن بودن خواسته به دادگاه و طرف مقابل کمک می‌کند حدود دعوا مشخص باشد.",
                 "اصطلاح کلیدی: خواسته — نتیجه یا امری که خواهان صدور حکم یا تصمیم درباره آن را از دادگاه درخواست می‌کند."},
                {"دلیل وسیله اثبات یا تقویت ادعای مطرح‌شده در حدود قانون است. سند، اقرار، شهادت و سایر ادله قانونی می‌توانند در اثبات موضوع نقش داشته باشند.",
                 "اصطلاح کلیدی: دلیل — وسیله‌ای که برای اثبات یا تقویت ادعا یا دفاع در چارچوب قانون ارائه می‌شود."}
        };

        final String[] questions = {
                "",
                "برای شروع یک دعوای مدنی، کدام اقدام در این پرونده نقش اصلی دارد؟",
                "در پرونده رضا، چه کسی عنوان خواهان دارد؟",
                "برای رفع ایراد دادخواست، کدام مورد باید با دقت بررسی شود؟",
                "در پرونده خسارت، خواسته به چه چیزی اشاره می‌کند؟",
                "در پرونده نگار، دلیل چه نقشی دارد؟"
        };

        final String[][] options = {
                {},
                {"تقدیم دادخواست از مسیر قانونی", "تماس تلفنی با طرف مقابل", "ارسال یک پیام غیررسمی"},
                {"شخصی که دادخواست علیه او مطرح شده", "شخصی که مطالبه وجه را مطرح کرده", "قاضی رسیدگی‌کننده"},
                {"رنگ جلد پرونده", "اطلاعات ضروری اصحاب دعوا و خواسته و دلایل", "شماره صندلی دادگاه"},
                {"نام ساختمان دادگستری", "زمان مراجعه به دفتر", "چیزی که خواهان از دادگاه درخواست می‌کند"},
                {"تزئین پرونده", "تعیین رنگ فرم دادخواست", "وسیله اثبات یا تقویت ادعا در چارچوب قانون"}
        };

        final int[] correctIndex = {0,0,1,1,2,2};
        final String[] explanations = {
                "",
                "طرح دعوا از مسیر قانونی و تقدیم دادخواست، نقطه شروع این پرونده است.",
                "در این پرونده رضا که مطالبه وجه را مطرح کرده، خواهان است؛ طرف مقابل خوانده است.",
                "مشخصات اصحاب دعوا، خواسته و دلایل از اطلاعات مهم دادخواست هستند و باید دقیق بررسی شوند.",
                "خواسته همان چیزی است که خواهان از دادگاه درخواست می‌کند؛ در اینجا جبران خسارت.",
                "دلیل برای اثبات یا تقویت ادعا در حدود قانون به کار می‌رود و سند یکی از انواع آن است."
        };

        public GameView(Context c) {
            super(c);
            setFocusable(true);
            prefs = c.getSharedPreferences("rah_edalat", Context.MODE_PRIVATE);
            officeBg = load(R.drawable.office_bg);
            libraryBg = load(R.drawable.library_bg);
            lawyer = load(R.drawable.char_lawyer);
            client = load(R.drawable.char_client);
            judge = load(R.drawable.char_judge);
            officer = load(R.drawable.char_officer);
            caseRoomAmir = load(R.drawable.case_room_amir);
            amirCloseup = load(R.drawable.case1_amir_closeup);
            amirEvidenceDialogue = load(R.drawable.case1_amir_evidence_dialogue);
            evidenceOpen = load(R.drawable.case1_evidence_open);
            phoneMessagesIcon = load(R.drawable.phone_messages_icon);
            phoneReceiptIcon = load(R.drawable.phone_receipt_icon);
            for(int i=0;i<22;i++){
                int id=getResources().getIdentifier(String.format(java.util.Locale.US,"receipt_piece_%02d",i+1),"drawable",getContext().getPackageName());
                receiptPieces[i]=load(id);
            }
            initReceiptPuzzle();
            tp.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        }

        Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            scale=Math.min(w/1080f,h/2400f);
            ox=(w-1080f*scale)/2f; oy=(h-2400f*scale)/2f;
            c.drawColor(Color.rgb(9,13,17));
            c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if(screen==START) drawStart(c); else {
                drawBackground(c);
                if(screen==OFFICE) drawOffice(c);
                else if(screen==LIBRARY) drawLibrary(c);
                else if(screen==CASE) drawCase(c);
                else if(screen==LESSON) drawLesson(c);
                else if(screen==QUIZ) drawQuiz(c);
                else if(screen==RESULT) drawResult(c);
                else if(screen==PROGRESS) drawProgress(c);
                else if(screen==ARCHIVE) drawArchive(c);
                else if(screen==LAWBOOK) drawLawBook(c);
                else if(screen==CASE_ROOM) drawCaseRoom(c);
                else if(screen==CASE1_AMIR) drawCase1Amir(c);
                else if(screen==CASE1_EVIDENCE) drawCase1Evidence(c);
                else if(screen==CASE1_PHONE) drawCase1Phone(c);
                else if(screen==CASE1_RECEIPTS) drawCase1Receipts(c);
                else if(screen==CASE1_DVD) drawCase1Dvd(c);
                drawTop(c);
            }
            if(settings) drawSettings(c);
            if(exitConfirm) drawExit(c);
            c.restore();
        }

        void drawBackground(Canvas c) {
            if(screen==CASE_ROOM || screen==CASE1_AMIR || screen==CASE1_EVIDENCE || screen==CASE1_PHONE || screen==CASE1_RECEIPTS || screen==CASE1_DVD) return;
            Bitmap b = screen==LIBRARY ? libraryBg : officeBg;
            if(b!=null) c.drawBitmap(b,null,new RectF(0,0,1080,2400),p);
            p.setColor(0x18000000); c.drawRect(0,0,1080,2400,p);
        }

        void drawStart(Canvas c) {
            p.setColor(0xff0d151b); c.drawRect(0,0,1080,2400,p);
            if(officeBg!=null){p.setAlpha(70);c.drawBitmap(officeBg,null,new RectF(0,0,1080,2400),p);p.setAlpha(255);}
            p.setColor(0x990a1116); c.drawRect(0,0,1080,2400,p);
            text(c,"راه عدالت",540,700,82,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"یادگیری آیین دادرسی مدنی در قالب پرونده و تصمیم",540,835,34,0xffeadfcd,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,300,1030,780,1260,34,0xff17435d);
            boxText(c,"ورود به دفتر وکیل",300,1030,780,1260,44,Color.WHITE,true);
            text(c,"ساخته شده توسط پارسا",540,2120,30,0xffd1c2ab,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawTop(Canvas c) {
            // Reserve a generous top safe zone so system status/gesture areas never cover game controls.
            p.setColor(0x9d101a21); c.drawRect(0,0,1080,275,p);
            if(screen!=OFFICE && screen!=START){
                round(c,34,165,154,265,46,0xdd17313f);
                boxText(c,"‹",34,165,154,265,58,Color.WHITE,true);
            }
            round(c,926,165,1046,265,46,0xdd17313f);
            boxText(c,"⚙",926,165,1046,265,38,Color.WHITE,true);
            boxText(c,screenTitle(),300,165,780,265,38,Color.WHITE,true);
        }

        String screenTitle(){
            switch(screen){
                case OFFICE:return "دفتر وکیل";
                case LIBRARY:return "کتابخانه پرونده‌ها";
                case CASE:return "پرونده جاری";
                case LESSON:return "یادگیری پرونده";
                case QUIZ:return "تصمیم حقوقی";
                case RESULT:return "نتیجه پرونده";
                case PROGRESS:return "پیشرفت";
                case ARCHIVE:return "بایگانی";
                case LAWBOOK:return "کتاب قانون";
                case CASE_ROOM:return "اتاق رسیدگی پرونده";
                case CASE1_AMIR:return "گفت‌وگو با امیر";
                case CASE1_EVIDENCE:return "محتویات کیف";
                case CASE1_PHONE:return "بررسی گوشی";
                case CASE1_RECEIPTS:return "پازل رسید بانکی";
                case CASE1_DVD:return "بررسی فیلم";
                default:return "راه عدالت";
            }
        }

        void drawOffice(Canvas c) {
            // No visible buttons or labels are placed over office objects.
            // The real objects in the background are the touch targets.
            boxText(c,"برای تعامل، خودِ اشیای دفتر را لمس کن",90,2180,990,2290,26,0xfff0e4cf,true);
        }

        void drawLibrary(Canvas c) {
            boxText(c,"برای باز کردن پرونده، خودِ پوشه را لمس کن",90,2150,990,2260,28,0xfff0e4cf,true);
            for(int i=1;i<=5;i++) drawFolder(c,i,folderX(i),folderY(i),i<=unlocked());
        }

        float folderX(int i){ return 155f + ((i-1)%3)*300f; }
        float folderY(int i){ return 520f + ((i-1)/3)*620f; }

        void drawFolder(Canvas c,int i,float x,float y,boolean open){
            int body=open?0xff7d5a36:0xff4c4b4a;
            round(c,x-92,y-75,x+92,y+85,18,body);
            round(c,x-72,y-93,x+5,y-55,12,body);
            text(c,"پرونده "+toFa(i),x,y-8,25,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,165);
            text(c,"●",x,y+40,22,open?0xffe3bd72:0xff8a8a8a,true,Layout.Alignment.ALIGN_CENTER,60);
        }

        void drawCaseRoom(Canvas c){
            drawPannedImage(c,caseRoomAmir,roomPan,120,2280);
            // Darken the bottom only to keep controls readable without hiding the scene.
            p.setColor(0x55000000); c.drawRect(0,2080,1080,2400,p);
            round(c,330,2070,750,2195,28,0xaa111b21);
            boxText(c,"برای دیدن اطراف، صفحه را چپ و راست بکش",340,2080,740,2140,24,Color.WHITE,true);
            boxText(c,"امیر را لمس کن",360,2140,720,2205,27,0xfff0dfbf,true);
        }

        void drawPannedImage(Canvas c, Bitmap b, float pan, float top, float bottom){
            if(b==null) return;
            float h=bottom-top, w=h*b.getWidth()/b.getHeight();
            float max=Math.max(0,w-1080f);
            float clamped=Math.max(0,Math.min(pan,max));
            c.drawBitmap(b,null,new RectF(-clamped,top,w-clamped,bottom),p);
        }

        void drawTopImagePreserveRatio(Canvas c, Bitmap b, float top, float targetWidth){
            if(b==null) return;
            float h=targetWidth*b.getHeight()/b.getWidth();
            c.drawBitmap(b,null,new RectF(0,top,targetWidth,top+h),p);
        }

        void drawCase1Amir(Canvas c){
            drawTopImagePreserveRatio(c,amirCloseup,120f,1080f);
            p.setColor(0x30000000); c.drawRect(0,120,1080,735,p);
            drawThoughtBubble(c,case1Dialogue==0 ? "ممنون که برای پرونده‌ام وقت گذاشتی." : "همه‌چیز از یک ماجرای مالی شروع شد.",760,260,360,240);
            panel(c,55,790,1025,2360,0xe9162027);
            if(case1Dialogue==0){
                boxText(c,"امیر",120,1585,960,1660,30,0xffe2bb78,true);
                boxText(c,"سلام. من امیرم. قبل از اینکه سراغ مدارک بریم، می‌خوام ماجرا رو برات توضیح بدم.",120,1680,960,1845,31,Color.WHITE,false);
                round(c,110,1940,970,2110,28,0xff17435d);
                boxText(c,"چه اتفاقی افتاده؟",110,1940,970,2110,34,Color.WHITE,true);
            } else if(case1Dialogue==1){
                boxText(c,"امیر",120,1585,960,1660,30,0xffe2bb78,true);
                boxText(c,"یک اختلاف مالی پیش اومده و می‌خوام بدونم از نظر حقوقی چه مدارکی به دردم می‌خوره.",120,1680,960,1845,31,Color.WHITE,false);
                round(c,110,1940,970,2110,28,0xff17435d);
                boxText(c,"چه مدرکی داری؟",110,1940,970,2110,34,Color.WHITE,true);
            } else {
                drawTopImagePreserveRatio(c,amirEvidenceDialogue,120f,1080f);
                panel(c,55,790,1025,2360,0xe9162027);
                boxText(c,"امیر",120,1585,960,1660,30,0xffe2bb78,true);
                boxText(c,"من بچمو خیلی دوس دارم. اون متولد ۱۳۹۰ هست؛ می‌خواستم برای تولدش یه دوچرخه بخرم که این مشکل پیش اومد.",120,1680,960,1870,30,Color.WHITE,false);
                round(c,110,1990,970,2180,28,0xff6d4f2d);
                boxText(c,"بررسی کیف",110,1990,970,2180,34,Color.WHITE,true);
                text(c,"حالا کیف قابل بررسی است.",540,2270,27,0xffe5d5bc,true,Layout.Alignment.ALIGN_CENTER,700);
            }
        }

        void drawThoughtBubble(Canvas c,String s,float cx,float cy,float w,float h){
            p.setColor(0xeefffdf8); c.drawOval(new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2),p);
            p.setColor(0xeefffdf8); c.drawCircle(cx-w*0.30f,cy+h*0.43f,24,p); c.drawCircle(cx-w*0.39f,cy+h*0.56f,13,p);
            text(c,s,cx,cy-48,27,0xff222222,true,Layout.Alignment.ALIGN_CENTER,w-45);
        }

        void drawCase1Evidence(Canvas c){
            p.setColor(0xff0b0f12); c.drawRect(0,0,1080,2400,p);
            if(evidenceOpen!=null){
                float top=145f, w=1080f, h=w*evidenceOpen.getHeight()/evidenceOpen.getWidth();
                c.drawBitmap(evidenceOpen,null,new RectF(0,top,w,top+h),p);
            }
            // Invisible interaction zones are used; the original image itself remains untouched.
            round(c,70,1860,1010,2285,30,0xe8142026);
            boxText(c,"روی گوشی، رسیدها یا DVD بزن تا بررسی شوند.",110,1870,970,1970,31,Color.WHITE,true);
            boxText(c,"پس از حل هر سه معما، آموزش دادخواست باز می‌شود.",110,1980,970,2070,27,0xffdfc9a5,true);
            boxText(c,(phoneSolved?"✓ گوشی بررسی شد":"○ گوشی")+"   "+(receiptSolved?"✓ رسید حل شد":"○ رسید")+"   "+(dvdSolved?"✓ فیلم بررسی شد":"○ فیلم"),110,2110,970,2200,29,0xffeadfce,true);
        }

        void drawCase1Phone(Canvas c){
            p.setColor(0xff070b0f); c.drawRect(0,0,1080,2400,p);
            // Realistic phone frame, with all interactive content kept inside the safe zone.
            round(c,155,300,925,2080,82,0xff0e1419);
            round(c,190,365,890,2015,58,0xff1b252c);
            round(c,455,335,625,370,18,0xff050708);
            if(!phoneSolved){
                boxText(c,"گوشی قفل است",190,420,890,560,44,Color.WHITE,true);
                boxText(c,"رمز را از گفت‌وگوی امیر پیدا کن.",220,565,860,645,29,0xffd8c7ad,false);
                boxText(c,phoneInput.length()==0?"• • • •":phoneDots(),250,690,830,800,48,Color.WHITE,true);
                String[][] keys={{"۱","۲","۳"},{"۴","۵","۶"},{"۷","۸","۹"},{"⌫","۰","✓"}};
                for(int r=0;r<4;r++)for(int col=0;col<3;col++){
                    float x=310+col*180,y=900+r*175;
                    round(c,x-62,y-62,x+62,y+62,34,0xff263944);
                    boxText(c,keys[r][col],x-62,y-62,x+62,y+62,37,Color.WHITE,true);
                }
                return;
            }
            if(phonePage==0){
                boxText(c,"صفحه اصلی",220,425,860,520,38,Color.WHITE,true);
                boxText(c,"امیر",235,530,845,605,25,0xffd9c49f,false);
                drawPhoneApp(c,phoneMessagesIcon,315,820,"پیام‌ها");
                drawPhoneApp(c,phoneReceiptIcon,765,820,"رسید واریز");
                round(c,285,1770,795,1905,28,0xff173f52);
                boxText(c,"بازگشت به محتویات کیف",285,1770,795,1905,29,Color.WHITE,true);
            } else if(phonePage==1){
                boxText(c,"پیام‌ها",220,425,860,520,38,Color.WHITE,true);
                phoneMessage(c,235,575,"امیر", "رسید واریز رو نگه دار؛ شاید لازم بشه مبلغ رو با مدارک تطبیق بدی.");
                phoneMessage(c,235,930,"علی رضایی", "مدرک رو تحویل گرفتم. بعداً درباره انتقال وجه صحبت می‌کنیم.");
                phoneMessage(c,235,1285,"امیر", "همون تراکنشی که امروز درباره‌اش حرف زدیم، ساعت ۱۵:۳۲ ثبت شده.");
                round(c,285,1770,795,1905,28,0xff173f52);
                boxText(c,"‹ بازگشت به صفحه اصلی",285,1770,795,1905,29,Color.WHITE,true);
            } else {
                boxText(c,"رسید واریز",220,425,860,520,38,Color.WHITE,true);
                round(c,235,575,845,1595,28,0xfff2efe8);
                boxText(c,"رسید انتقال وجه",270,625,810,715,34,0xff20272b,true);
                boxText(c,"تاریخ: ۱۴۰۴/۰۶/۲۵",270,760,810,825,26,0xff33383c,false);
                boxText(c,"ساعت: ۱۵:۳۲:۴۷",270,840,810,905,26,0xff33383c,false);
                boxText(c,"مبلغ: ۱٬۰۰۰٬۰۰۰٬۰۰۰ ریال",270,930,810,1015,31,0xff202427,true);
                boxText(c,"نوع: انتقال وجه کارت به کارت",270,1040,810,1115,25,0xff33383c,false);
                boxText(c,"شماره پیگیری: ۳۸۷۶۵۴۳۲۱",270,1140,810,1215,25,0xff33383c,false);
                boxText(c,"کارت مقصد: ۶۲۱۹-۸۶۱۰-۷۴۳۲-۱۰۸۹",270,1240,810,1315,23,0xff33383c,false);
                boxText(c,"بانک مقصد: بانک ملت",270,1340,810,1415,25,0xff33383c,false);
                round(c,285,1770,795,1905,28,0xff173f52);
                boxText(c,"‹ بازگشت به صفحه اصلی",285,1770,795,1905,29,Color.WHITE,true);
            }
        }

        void drawPhoneApp(Canvas c, Bitmap icon, float cx, float cy, String label){
            if(icon!=null)c.drawBitmap(icon,null,new RectF(cx-105,cy-105,cx+105,cy+105),p);
            else {round(c,cx-105,cy-105,cx+105,cy+105,38,0xff204a5e); boxText(c,label,cx-105,cy-30,cx+105,cy+30,26,Color.WHITE,true);}
            boxText(c,label,cx-145,cy+120,cx+145,cy+185,27,Color.WHITE,true);
        }

        void phoneMessage(Canvas c,float l,float t,String who,String msg){
            round(c,l,t,l+610,t+260,28,0xff22343d);
            boxText(c,who,l+25,t+18,l+585,t+70,24,0xffe4bd75,true);
            boxText(c,msg,l+25,t+78,l+585,t+230,25,Color.WHITE,false);
        }

        String phoneDots(){
            StringBuilder b=new StringBuilder();
            for(int i=0;i<4;i++) b.append(i<phoneInput.length()?"●":"○").append(" ");
            return b.toString();
        }

        void drawCase1Dvd(Canvas c){
            p.setColor(0xff0b0f12); c.drawRect(0,0,1080,2400,p);
            round(c,90,220,990,1740,34,0xff121a20);
            if(dvdSolved){
                round(c,145,330,935,1290,26,0xff050708);
                text(c,"فریم کلیدی فیلم",540,510,40,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
                round(c,220,650,860,1030,18,0xff1d252a);
                text(c,"خودروی تیره در ورودی پشتی\nفردی بسته‌ای را تحویل می‌دهد.",540,800,32,0xfff0e5d4,true,Layout.Alignment.ALIGN_CENTER,600);
                text(c,"زمان تقریبی: ۱۵:۳۴",540,1120,29,0xffd8bc91,true,Layout.Alignment.ALIGN_CENTER,700);
                round(c,250,1900,830,2070,28,0xff17435d);
                text(c,"بازگشت به محتویات کیف",540,2010,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,560);
                return;
            }
            text(c,"فیلم مدرک را بررسی کن",540,430,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
            text(c,"نوار زمان را حرکت بده و لحظه مرتبط با تراکنش را پیدا کن.",540,560,29,0xffddcbb0,true,Layout.Alignment.ALIGN_CENTER,820);
            round(c,180,900,900,980,20,0xff263640);
            round(c,210,918,870,962,16,0xff56717e);
            float marker=260;
            round(c,marker-18,885,marker+18,995,18,0xffe0bb72);
            text(c,"۱۵:۳۰",210,1080,27,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,140);
            text(c,"۱۵:۳۵",870,1080,27,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,140);
            round(c,270,1190,810,1350,28,0xff17435d);
            text(c,"بررسی لحظه انتخاب‌شده",540,1290,30,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
            round(c,250,1920,830,2070,28,0xff17435d);
            text(c,"بازگشت به محتویات کیف",540,2010,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,560);
        }

        void drawCase1Receipts(Canvas c){
            p.setColor(0xff0b0f12); c.drawRect(0,0,1080,2400,p);
            round(c,35,300,1045,1230,30,0xff18242b);
            boxText(c,"کادر حل پازل رسید",70,330,1010,405,32,0xffe5c58f,true);
            // Six columns × four rows form the real receipt silhouette; two merged tiles keep the total at 22 pieces.
            for(int i=0;i<22;i++){
                float tx=targetX(i), ty=targetY(i);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f); p.setColor(0x554f6570);
                if(i>=20) c.drawRoundRect(tx-150,ty-82,tx+150,ty+82,14,14,p);
                else c.drawRoundRect(tx-70,ty-82,tx+70,ty+82,14,14,p);
                p.setStyle(Paint.Style.FILL);
            }
            for(int i=0;i<22;i++) if(receiptOnBoard[i] && receiptPieces[i]!=null) drawReceiptPiece(c,i,receiptPieceX[i],receiptPieceY[i],155f,receiptRot[i]);

            round(c,35,1265,1045,1820,30,0xff131c21);
            boxText(c,"تکه‌های به‌هم‌ریخته",70,1295,1010,1370,31,0xffe5c58f,true);
            for(int i=0;i<22;i++) if(!receiptOnBoard[i]){
                drawReceiptPiece(c,i,receiptTrayX[i],receiptTrayY[i],105f,0);
                if(i==selectedReceiptPiece){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xffe6bd70);c.drawRoundRect(receiptTrayX[i]-58,receiptTrayY[i]-58,receiptTrayX[i]+58,receiptTrayY[i]+58,16,16,p);p.setStyle(Paint.Style.FILL);}
            }
            boxText(c,"تکه انتخاب‌شده: "+(selectedReceiptPiece<0?"هیچ‌کدام":toFa(selectedReceiptPiece+1)),70,1810,1010,1860,24,0xffe4d8c9,true);
            // Separate control zones: arrows and rotation never overlap.
            drawArrowButton(c,540,2035,"▲");
            drawArrowButton(c,540,2140,"▼");
            drawArrowButton(c,360,2090,"◀");
            drawArrowButton(c,720,2090,"▶");
            round(c,455,1880,625,1960,24,0xff6b4f2e);
            boxText(c,"↻",455,1880,625,1960,34,Color.WHITE,true);
        }

        float targetX(int i){
            if(i<20){ int col=i%6,row=i/6; return 105+col*145; }
            if(i==20)return 468;
            return 758;
        }
        float targetY(int i){
            if(i<20){ int col=i%6,row=i/6; return 450+row*150; }
            return 900;
        }

        void drawArrowButton(Canvas c,float x,float y,String s){round(c,x-65,y-55,x+65,y+55,25,0xff263b46);text(c,s,x,y+16,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,110);}

        void drawReceiptPiece(Canvas c,int i,float cx,float cy,float maxSize,float rot){
            Bitmap b=receiptPieces[i]; if(b==null)return;
            float ratio=Math.min(maxSize/b.getWidth(),maxSize/b.getHeight());
            float w=b.getWidth()*ratio,h=b.getHeight()*ratio;
            c.save(); c.rotate(rot,cx,cy); c.drawBitmap(b,null,new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2),p); c.restore();
        }

        void initReceiptPuzzle(){
            for(int i=0;i<22;i++){ receiptOnBoard[i]=false; receiptLocked[i]=false; receiptRot[i]=0; }
            // Deterministic shuffled tray order so pieces are never presented in solved order.
            int[] order={13,2,19,7,21,4,16,0,10,18,5,14,8,20,1,12,17,6,11,3,15,9};
            float[] xs={125,285,445,605,765,925,125,285,445,605,765,925,205,365,525,685,845,1005,205,365,525,685};
            float[] ys={1410,1410,1410,1410,1410,1410,1550,1550,1550,1550,1550,1550,1690,1690,1690,1690,1690,1690,1770,1770,1770,1770};
            for(int slot=0;slot<22;slot++){ int piece=order[slot]; receiptTrayX[piece]=xs[slot]; receiptTrayY[piece]=ys[slot]; }
            // Targets correspond to the actual clean receipt composition.
            for(int i=0;i<22;i++){ receiptPieceX[i]=540; receiptPieceY[i]=690; }
            selectedReceiptPiece=-1; receiptSolved=false;
        }

        void drawCase(Canvas c) {
            int step=Math.min(storyStep,3);
            Bitmap person=caseCharacter(selectedCase);
            float targetH=step==0?760:690;
            drawCharacter(c,person,790,1450,targetH);
            panel(c,55,1540,1025,2240,0xe90c171e);
            text(c,"پرونده "+toFa(selectedCase)+"  |  "+casePeople[selectedCase],965,1630,28,0xffe0b96e,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,caseIssues[selectedCase],965,1715,30,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,story[selectedCase][step],965,1845,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,100,2090,980,2215,30,0xff17435d);
            boxText(c,step<3?"ادامه روایت":"ورود به بخش آموزشی",100,2090,980,2215,34,Color.WHITE,true);
        }

        Bitmap caseCharacter(int n){
            if(n==2 || n==5) return officer;
            if(n==4) return judge;
            if(n==3) return lawyer;
            return client;
        }

        void drawLesson(Canvas c) {
            panel(c,55,230,1025,2220,0xe9101920);
            text(c,"مفهوم حقوقی پرونده",970,390,40,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            text(c,lesson[selectedCase][0],970,510,34,0xfff2eadc,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,90,1330,990,1670,28,0xff1b3340);
            text(c,lesson[selectedCase][1],930,1435,29,0xffead9bc,true,Layout.Alignment.ALIGN_OPPOSITE,820);
            text(c,"حالا سؤال پرونده را حل کن.",540,1815,32,0xffe0c38f,true,Layout.Alignment.ALIGN_CENTER,800);
            round(c,120,1900,960,2070,30,0xff17435d);
            boxText(c,"ورود به تصمیم حقوقی",120,1900,960,2070,36,Color.WHITE,true);
        }

        void drawQuiz(Canvas c) {
            panel(c,45,220,1035,2225,0xe9101920);
            text(c,questions[selectedCase],960,390,36,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            for(int i=0;i<3;i++){
                float top=650+i*360;
                int color=answerChosen ? (i==correctIndex[selectedCase]?0xff235c43:0xff422a2c) : 0xff173c51;
                round(c,95,top,985,top+245,28,color);
                boxText(c,options[selectedCase][i],125,top,955,top+245,31,Color.WHITE,true);
                if(answerChosen && i==correctIndex[selectedCase]) text(c,"✓",130,top+155,38,0xffb7e7c8,true,Layout.Alignment.ALIGN_CENTER,70);
            }
            if(answerChosen){
                text(c,correct?"پاسخ درست است":"پاسخ انتخابی درست نیست",540,1785,35,correct?0xff9ee0ba:0xffffb5ad,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,explanations[selectedCase],930,1880,29,0xffe7dac8,true,Layout.Alignment.ALIGN_OPPOSITE,820);
                round(c,170,2070,910,2185,28,0xff294552);
                text(c,"مشاهده نتیجه",540,2145,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            }
        }

        void drawResult(Canvas c) {
            panel(c,75,420,1005,1980,0xe90f1920);
            text(c,correct?"پرونده با موفقیت پیش رفت":"نیاز به مرور و تلاش دوباره",540,720,48,correct?0xffd8eddb:0xffffc1ba,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,correct?"مفهوم این پرونده به بخش پیشرفت اضافه شد.":"پاسخ را مرور کن و دوباره تصمیم بگیر.",540,835,31,0xffe5d9c8,true,Layout.Alignment.ALIGN_CENTER,850);
            round(c,150,1080,930,1250,30,0xff17435d);
            text(c,correct?(selectedCase<5?"بازگشت به کتابخانه":"پایان پنج پرونده") : "تلاش دوباره",540,1185,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
        }

        void drawProgress(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            int completed=prefs.getInt("completed",0);
            text(c,"پیشرفت آموزشی",540,510,46,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"پرونده‌های تکمیل‌شده: "+toFa(completed)+" از ۵",540,700,38,0xffeadfcf,true,Layout.Alignment.ALIGN_CENTER,850);
            for(int i=1;i<=5;i++){
                float y=850+(i-1)*210;
                round(c,150,y,930,y+150,24,i<=completed?0xff23543f:0xff263139);
                text(c,"پرونده "+toFa(i),850,y+62,30,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,650);
                text(c,i<=completed?"تکمیل شده":"در انتظار یادگیری",850,y+105,23,0xffd2c6b6,true,Layout.Alignment.ALIGN_OPPOSITE,650);
            }
        }

        void drawArchive(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            text(c,"بایگانی پرونده‌ها",540,510,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            int completed=prefs.getInt("completed",0);
            if(completed==0){text(c,"هنوز پرونده‌ای تکمیل نشده است.",540,800,32,0xffd8cdbd,true,Layout.Alignment.ALIGN_CENTER,850);return;}
            for(int i=1;i<=completed;i++){
                float y=700+(i-1)*245;
                text(c,"پرونده "+toFa(i),900,y,32,0xffe0bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,700);
                text(c,titles[i],900,y+55,27,0xffeee5d8,true,Layout.Alignment.ALIGN_OPPOSITE,700);
            }
        }

        void drawLawBook(Canvas c) {
            panel(c,65,300,1015,2180,0xe90e1820);
            text(c,"کتاب قانون",540,440,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"راهنمای سریع مفاهیم این پنج پرونده",540,540,29,0xffd5bd98,true,Layout.Alignment.ALIGN_CENTER,850);
            String[] terms={"دادخواست","خواهان و خوانده","اجزای دادخواست","خواسته","دلیل"};
            String[] defs={"وسیله طرح دعوا از مسیر قانونی.","نقش طرفین دعوا را مشخص می‌کند.","اطلاعات ضروری برای شناسایی و بیان دعوا.","چیزی که خواهان از دادگاه درخواست می‌کند.","وسیله اثبات یا تقویت ادعا در حدود قانون."};
            for(int i=0;i<5;i++){
                float y=700+i*270;
                text(c,terms[i],930,y,32,0xffe1bb74,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,defs[i],930,y+70,27,0xffeee6d9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
            }
        }

        void drawSettings(Canvas c) {
            p.setColor(0x88000000); c.drawRect(0,0,1080,2400,p);
            panel(c,130,600,950,1800,0xf017252d);
            text(c,"تنظیمات",540,750,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            round(c,190,900,890,1080,30,0xff173c51);
            text(c,prefs.getBoolean("music",true)?"موسیقی: روشن":"موسیقی: خاموش",540,1010,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1160,890,1340,30,0xff294552);
            text(c,"افکت صدا: روشن",540,1270,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1420,890,1600,30,0xff3b2729);
            text(c,"خروج از بازی",540,1530,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawExit(Canvas c) {
            p.setColor(0xaa000000); c.drawRect(0,0,1080,2400,p);
            panel(c,140,900,940,1510,0xff17242c);
            text(c,"از بازی خارج می‌شوی؟",540,1040,42,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,750);
            round(c,200,1160,520,1310,28,0xff294552); text(c,"خیر",360,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
            round(c,560,1160,880,1310,28,0xff4a282b); text(c,"بله",720,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
        }

        void drawCharacter(Canvas c, Bitmap b, float cx, float bottom, float targetH) {
            if(b==null)return;
            float ratio=targetH/b.getHeight();
            float w=b.getWidth()*ratio;
            RectF d=new RectF(cx-w/2,bottom-targetH,cx+w/2,bottom);
            p.setAlpha(255); c.drawBitmap(b,null,d,p);
        }

        void round(Canvas c,float l,float t,float r,float b,float rad,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        void panel(Canvas c,float l,float t,float r,float b,int color){round(c,l,t,r,b,34,color);}

        void boxText(Canvas c,String s,float l,float t,float r,float b,float size,int color,boolean bold){
            float w=r-l;
            tp.setTextSize(size); tp.setColor(color); tp.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            StaticLayout sl=StaticLayout.Builder.obtain(s,0,s.length(),tp,(int)w)
                    .setAlignment(Layout.Alignment.ALIGN_OPPOSITE).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(true).build();
            float tx=l, ty=t+(b-t-sl.getHeight())/2f;
            c.save(); c.translate(tx,ty); sl.draw(c); c.restore();
        }

        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Layout.Alignment align,float width){
            tp.setTextSize(size); tp.setColor(color); tp.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            StaticLayout sl=StaticLayout.Builder.obtain(s,0,s.length(),tp,(int)width)
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(true).build();
            float tx = align==Layout.Alignment.ALIGN_CENTER ? x-width/2f : x-width;
            c.save(); c.translate(tx,y); sl.draw(c); c.restore();
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=(e.getX()-ox)/scale, y=(e.getY()-oy)/scale;
            if(screen==CASE_ROOM){
                return handleCaseRoomTouch(e,x,y);
            }
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;

            if(exitConfirm){
                if(in(x,y,560,1160,880,1310)){finishActivity();return true;}
                if(in(x,y,200,1160,520,1310)){exitConfirm=false;invalidate();return true;}
                return true;
            }
            if(settings){
                if(in(x,y,190,900,890,1080)){toggleMusic();invalidate();return true;}
                if(in(x,y,190,1160,890,1340)){Toast.makeText(getContext(),"افکت صدا در نسخه پایه فعال است.",Toast.LENGTH_SHORT).show();return true;}
                if(in(x,y,190,1420,890,1600)){askExit();return true;}
                if(in(x,y,60,0,1020,560)){settings=false;invalidate();return true;}
                return true;
            }

            if(screen==START){if(in(x,y,300,1030,780,1260)){goTo(OFFICE);}return true;}

            // Large, separated top controls: back on left, settings on right.
            if(screen!=OFFICE && in(x,y,25,155,175,280)){
                if(screen==CASE1_AMIR){history.clear();history.push(LIBRARY);screen=CASE_ROOM;stopMusic();invalidate();return true;}
                goBack();return true;
            }
            if(in(x,y,900,155,1060,280)){settings=true;invalidate();return true;}

            if(screen==OFFICE){
                if(in(x,y,0,280,520,1370)){goTo(LIBRARY);return true;}
                if(in(x,y,650,820,930,1560)){goTo(ARCHIVE);return true;}
                if(in(x,y,850,1450,1080,2200)){goTo(PROGRESS);return true;}
                if(in(x,y,0,1760,310,2220)){goTo(LAWBOOK);return true;}
                if(in(x,y,300,1660,820,2300)){goTo(LIBRARY);return true;}
            }
            else if(screen==LIBRARY){
                for(int i=1;i<=5;i++){
                    float cx=folderX(i), cy=folderY(i);
                    if(in(x,y,cx-125,cy-125,cx+125,cy+125)){
                        if(i<=unlocked()){
                            selectedCase=i;
                            if(i==1){resetCase1New();goTo(CASE_ROOM);} else {storyStep=0;goTo(CASE);}
                        } else Toast.makeText(getContext(),"این پرونده هنوز قفل است.",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
            else if(screen==CASE_ROOM){
                int action=e.getAction();
                if(action==MotionEvent.ACTION_DOWN){roomDownX=x;roomStartPan=roomPan;roomDragging=false;return true;}
                if(action==MotionEvent.ACTION_MOVE){
                    float dx=x-roomDownX;
                    if(Math.abs(dx)>12) roomDragging=true;
                    roomPan=Math.max(0,Math.min(roomMaxPan(),roomStartPan-dx));
                    invalidate(); return true;
                }
                if(action==MotionEvent.ACTION_UP){
                    float dx=x-roomDownX;
                    if(Math.abs(dx)>25 || roomDragging){roomDragging=false;return true;}
                    float h=2160f, imgW=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
                    float ix=(x+roomPan)/imgW*caseRoomAmir.getWidth();
                    float iy=(y-120)/h*caseRoomAmir.getHeight();
                    if(ix>=240 && ix<=780 && iy>=200 && iy<=700){case1Dialogue=0;goTo(CASE1_AMIR);return true;}
                    return true;
                }
                return true;
            }
            else if(screen==CASE1_AMIR){
                if(case1Dialogue==2 && (in(x,y,515,360,790,650) || in(x,y,70,1950,1010,2220))){goTo(CASE1_EVIDENCE);return true;}
                if(in(x,y,100,1970,980,2200)){
                    if(case1Dialogue<2){case1Dialogue++; invalidate();}
                    return true;
                }
            }
            else if(screen==CASE1_EVIDENCE){
                // exact open-bag image: phone / receipts / DVD hitboxes mapped to the displayed 16:9 image.
                if(in(x,y,180,350,445,590)){goTo(CASE1_PHONE);return true;}
                if(in(x,y,395,415,625,690)){goTo(CASE1_RECEIPTS);return true;}
                if(in(x,y,610,415,840,635)){goTo(CASE1_DVD);return true;}
            }
            else if(screen==CASE1_PHONE){
                if(!phoneSolved){
                    String[][] keys={{"۱","۲","۳"},{"۴","۵","۶"},{"۷","۸","۹"},{"⌫","۰","✓"}};
                    for(int r=0;r<4;r++)for(int col=0;col<3;col++) if(in(x,y,248+col*180,838+r*175,372+col*180,962+r*175)){
                        String k=keys[r][col];
                        if(k.equals("⌫")){if(phoneInput.length()>0)phoneInput=phoneInput.substring(0,phoneInput.length()-1);}
                        else if(k.equals("✓")){if(phoneInput.equals("۱۳۹۰")){phoneSolved=true;phonePage=0;checkCase1Puzzles();}}
                        else if(phoneInput.length()<4)phoneInput+=k;
                        invalidate(); return true;
                    }
                } else {
                    if(phonePage==0){
                        if(in(x,y,180,680,520,1050)){phonePage=1;invalidate();return true;}
                        if(in(x,y,560,680,950,1050)){phonePage=2;invalidate();return true;}
                        if(in(x,y,260,1740,820,1935)){goTo(CASE1_EVIDENCE);return true;}
                    } else if(phonePage==1 || phonePage==2){
                        if(in(x,y,260,1740,820,1935)){phonePage=0;invalidate();return true;}
                    }
                }
            }
            else if(screen==CASE1_DVD){
                if(in(x,y,230,1850,850,2110)){dvdSolved=true;checkCase1Puzzles();if(screen!=LESSON)goTo(CASE1_EVIDENCE);return true;}
                if(!dvdSolved && in(x,y,150,850,930,1020)){dvdSolved=true;checkCase1Puzzles();invalidate();return true;}
            }
            else if(screen==CASE1_RECEIPTS){
                handleReceiptTouch(x,y); return true;
            }
            else if(screen==CASE){
                if(in(x,y,80,2040,1000,2250)){
                    if(storyStep<3){storyStep++;invalidate();} else goTo(LESSON);
                    return true;
                }
            }
            else if(screen==LESSON){if(in(x,y,100,1850,980,2100)){answerChosen=false;goTo(QUIZ);return true;}}
            else if(screen==QUIZ){
                if(answerChosen && in(x,y,120,2020,960,2210)){goTo(RESULT);return true;}
                if(!answerChosen){
                    for(int i=0;i<3;i++) if(in(x,y,80,630+i*360,1000,920+i*360)){
                        answerChosen=true; correct=i==correctIndex[selectedCase]; invalidate(); return true;
                    }
                }
            }
            else if(screen==RESULT){
                if(in(x,y,120,1050,960,1280)){
                    if(correct){
                        int done=prefs.getInt("completed",0);
                        if(selectedCase>done) prefs.edit().putInt("completed",selectedCase).putInt("unlocked",Math.min(5,selectedCase+1)).apply();
                        history.clear();
                        history.push(OFFICE);
                        screen=LIBRARY;
                        stopMusic();
                        invalidate();
                    } else {
                        answerChosen=false;
                        if(!history.isEmpty() && history.peek()==QUIZ) history.pop();
                        screen=QUIZ;
                        invalidate();
                    }
                    return true;
                }
            }
            return true;
        }

        void handleReceiptTouch(float x,float y){
            if(in(x,y,450,1870,630,1970) && selectedReceiptPiece>=0 && !receiptLocked[selectedReceiptPiece]){
                receiptRot[selectedReceiptPiece]=(receiptRot[selectedReceiptPiece]+90)%360; invalidate(); return;
            }
            if(selectedReceiptPiece>=0 && !receiptLocked[selectedReceiptPiece]){
                float dx=0,dy=0;
                if(in(x,y,475,1980,605,2090))dy=-28;
                else if(in(x,y,475,2085,605,2195))dy=28;
                else if(in(x,y,295,2035,425,2145))dx=-28;
                else if(in(x,y,655,2035,785,2145))dx=28;
                if(dx!=0||dy!=0){receiptPieceX[selectedReceiptPiece]+=dx;receiptPieceY[selectedReceiptPiece]+=dy;checkReceiptPiece(selectedReceiptPiece);invalidate();return;}
            }
            for(int i=0;i<22;i++) if(!receiptOnBoard[i]){
                if(in(x,y,receiptTrayX[i]-70,receiptTrayY[i]-70,receiptTrayX[i]+70,receiptTrayY[i]+70)){
                    selectedReceiptPiece=i; receiptOnBoard[i]=true;
                    receiptPieceX[i]=540; receiptPieceY[i]=700; receiptRot[i]=0; invalidate(); return;
                }
            }
            for(int i=0;i<22;i++) if(receiptOnBoard[i] && !receiptLocked[i]){
                if(in(x,y,receiptPieceX[i]-95,receiptPieceY[i]-95,receiptPieceX[i]+95,receiptPieceY[i]+95)){ selectedReceiptPiece=i; invalidate(); return; }
            }
        }

        void checkReceiptPiece(int i){
            if(i<0 || receiptLocked[i])return;
            if(receiptRot[i]%360!=0)return;
            float dx=receiptPieceX[i]-targetX(i),dy=receiptPieceY[i]-targetY(i);
            if(Math.abs(dx)<=38 && Math.abs(dy)<=38){
                receiptPieceX[i]=targetX(i); receiptPieceY[i]=targetY(i); receiptLocked[i]=true;
                checkCase1Puzzles();
            }
        }

        void checkCase1Puzzles(){
            boolean all=phoneSolved && dvdSolved;
            if(all){
                all=true;
                for(int i=0;i<22;i++) if(!receiptLocked[i]){all=false;break;}
            }
            receiptSolved=allReceiptLocked();
            if(phoneSolved && receiptSolved && dvdSolved){
                // The three evidence puzzles are the gate. Only after all three are solved does education open.
                history.clear();
                history.push(CASE1_EVIDENCE);
                answerChosen=false; correct=false; selectedCase=1;
                screen=LESSON; startMusic(); invalidate();
            }
        }

        boolean allReceiptLocked(){for(int i=0;i<22;i++)if(!receiptLocked[i])return false;return true;}

        boolean handleCaseRoomTouch(MotionEvent e,float x,float y){
            if(e.getAction()==MotionEvent.ACTION_DOWN){roomDownX=x;roomStartPan=roomPan;roomDragging=false;return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                float dx=x-roomDownX;
                if(Math.abs(dx)>12) roomDragging=true;
                roomPan=Math.max(0,Math.min(roomMaxPan(),roomStartPan-dx));
                invalidate(); return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP){
                float dx=x-roomDownX;
                if(Math.abs(dx)>25 || roomDragging){roomDragging=false;return true;}
                if(in(x,y,25,155,175,280)){goBack();return true;}
                if(in(x,y,900,155,1060,280)){settings=true;invalidate();return true;}
                float h=2160f;
                float imgW=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
                float ix=(x+roomPan)/imgW*caseRoomAmir.getWidth();
                float iy=(y-120)/h*caseRoomAmir.getHeight();
                if(ix>=240 && ix<=780 && iy>=190 && iy<=710){case1Dialogue=0;goTo(CASE1_AMIR);return true;}
                return true;
            }
            return true;
        }

        float roomMaxPan(){
            if(caseRoomAmir==null)return 0;
            float h=2160f;
            float w=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
            return Math.max(0,w-1080f);
        }

        boolean in(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
        int unlocked(){return Math.max(1,Math.min(5,prefs.getInt("unlocked",1)));}
        String toFa(int n){return String.valueOf(n).replace("0","۰").replace("1","۱").replace("2","۲").replace("3","۳").replace("4","۴").replace("5","۵").replace("6","۶").replace("7","۷").replace("8","۸").replace("9","۹");}

        void resetCase1New(){
            case1Dialogue=0;
            phoneInput=""; phonePage=0; phoneSolved=false; receiptSolved=false; dvdSolved=false;
            initReceiptPuzzle();
            roomPan=700f;
            roomDragging=false;
            roomDownX=0f;
            roomStartPan=700f;
        }

        void goTo(int next){
            if(screen!=next) history.push(screen);
            screen=next;
            if(next==LESSON||next==QUIZ||next==CASE||next==CASE_ROOM||next==CASE1_AMIR||next==CASE1_EVIDENCE||next==CASE1_PHONE||next==CASE1_RECEIPTS||next==CASE1_DVD) startMusic(); else stopMusic();
            invalidate();
        }

        boolean goBack(){
            if(exitConfirm||settings){exitConfirm=false;settings=false;invalidate();return true;}
            if(history.isEmpty())return false;
            screen=history.pop(); stopMusic(); invalidate(); return true;
        }

        void askExit(){exitConfirm=true;settings=false;invalidate();}
        void finishActivity(){((Activity)getContext()).finish();}

        void toggleMusic(){
            boolean on=prefs.getBoolean("music",true);
            prefs.edit().putBoolean("music",!on).apply();
            if(on) stopMusic(); else startMusic();
        }

        void startMusic(){
            if(!prefs.getBoolean("music",true))return;
            stopMusic();
            try{
                music=MediaPlayer.create(getContext(),R.raw.stage_music);
                if(music!=null){music.setLooping(true);music.setVolume(.55f,.55f);music.start();}
            }catch(Exception ignored){music=null;}
        }

        void stopMusic(){
            if(music!=null){try{music.stop();}catch(Exception ignored){} try{music.release();}catch(Exception ignored){} music=null;}
        }
    }
}
