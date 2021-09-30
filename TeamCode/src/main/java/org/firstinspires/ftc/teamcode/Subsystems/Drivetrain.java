package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import static java.lang.Thread.sleep;

public class Lift {
    //Define Hardware Objects
    public DcMotor  Lift          =   null;
    public Servo    Wrist         =   null;
    public Servo    Pivot         =   null;


    //Constants Lift

    private static final double     LIFTSPEED       =   0.78;

    private static final double     LIFTUP          =   14.8; //Number is in inches 13 is too low
    private static final double     LIFTDOWN        =   0; //To make sure it goes all the way down
    private static final int        LIFTPARTIAL        = 8;

    private static final double     TICKS_PER_LIFT_IN = 100; // determined experimentally
    private static final int        LIFT_HEIGHT_HIGH = (int) (LIFTUP * TICKS_PER_LIFT_IN); // converts to ticks
    private static final int        LIFT_HEIGHT_LOW = (int) (LIFTDOWN * TICKS_PER_LIFT_IN); // converts to ticks
    private static final int        LIFT_HEIGHT_PARTIAL = (int) (LIFTPARTIAL * TICKS_PER_LIFT_IN); // converts to ticks
    //Constants Wrist
    private static final double     WRISTINIT         =   0.4;// 0.35 for V3
    private static final double     WRISTOPEN         =   0.78;//0.3 for V3
    private static final double     WRISTSUPEROPEN    =   0.01;//0.01 for V3
    private static final double     WRISTCLOSE        =   0.22;// 0.8 for V3
    private static final double     WRISTPARTOPEN     =   0.55;// 0.8 for V3

    //Constants Pivot
    public static final double      PIVOTSTART          =   .33 ;
    public static final double      PIVOTUP             =   0.40 ;
    public static final double      PIVOTDOWN           =   0.65;
    public static final double      PIVOTHALFWAY        =   0.50;

    public void init(HardwareMap hwMap)  {
        Lift=hwMap.get(DcMotor.class,"Lift");
        Wrist=hwMap.get(Servo.class,"Wrist");
        Pivot=hwMap.get(Servo.class,"Pivot");

        //Positive=up and Negative=down
        Lift.setDirection(DcMotor.Direction.FORWARD);
        Lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        Lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        Wrist.setPosition(WRISTINIT);
        Pivot.setPosition(PIVOTSTART);

    }

    //// Single operation methods - see below for methods to be called in Opmodes
    public void LiftRise() {
        Lift.setTargetPosition(LIFT_HEIGHT_HIGH);// value is in ticks from above calculation
        Lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        Lift.setPower(LIFTSPEED);
    }
    public void LiftLower() {
        Lift.setTargetPosition(LIFT_HEIGHT_LOW); // this one is just zero for now
        Lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        Lift.setPower(LIFTSPEED);
    }
    public void LiftMiddle() {
        Lift.setTargetPosition(LIFTPARTIAL);
        Lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        Lift.setPower(LIFTSPEED);
    }

    public void LiftIdle() {
        Lift.setPower(0);
    }

    public double getLiftHeight() {
        double liftHeight;
        Lift.getCurrentPosition();
        liftHeight =  Lift.getCurrentPosition() *  TICKS_PER_LIFT_IN;
        return  liftHeight;
    }



    public void WristOpen()  {

        Wrist.setPosition(WRISTOPEN);
    }

    public void WristPartOpen() {
        Wrist.setPosition(WRISTPARTOPEN);
    }

    public void WRISTSuperOpen()  {

        Wrist.setPosition(WRISTSUPEROPEN);
    }
    public void WristClose() {

        Wrist.setPosition(WRISTCLOSE);
    }



    public void PivotDown()  {

        Pivot.setPosition(PIVOTDOWN);
    }

    public void PivotUp() {

        Pivot.setPosition(PIVOTUP);
    }
    public void PivotStart() {

        Pivot.setPosition(PIVOTSTART);

    }

    public void PivotHalfWay() {

        Pivot.setPosition(PIVOTHALFWAY);
    }

    ///// Multi Function methods to be called by the Opmodes

    public void resetWobble() {
        WristClose();
        LiftLower();
    }

    public void readyToGrabWobble() {
        //LiftRise();
        //ArmExtend();
        WristOpen();
        LiftLower();
    }

    public void grabAndLift() {
        WristClose();
        //LiftRise();
    }

    public void lowerAndRelease() {
        LiftLower();
        WristOpen();
    }
}

