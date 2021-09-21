package org.firstinspires.ftc.teamcode.Auto;

import android.view.View;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.Subsystems.Drivetrain;



@Autonomous(name="Basic GunShow Opmodec", group="Auto")
@Disabled

// This opmode is meant to be extend in teleop and autonomus opmodes.
// This enables code reuse and a single source to edit the common methods and constants.


public class BasicGunShow extends LinearOpMode {
    /* Declare OpMode members. */

    // When the drivetrain is instatiated from the drivetrain subsystem, it automatically assumes an autonomous opMode.

    public Drivetrain drivetrain  = new Drivetrain();   // Use subsystem Drivetrain
    // "SideServo is a class in the subsystem package that is used to create "sideServo" it is case sensitive so
    // the two are not the same. "sideServo" is the object we create ad use.



    // Timers and time limits for each timer
    public ElapsedTime          PIDtimer    = new ElapsedTime(); // PID loop timer
    public ElapsedTime          drivetime   = new ElapsedTime(); // timeout timer for driving
    NormalizedColorSensor colorSensor; // copied in from color sensor sample code
    View relativeLayout; // copied in from color sensor sample code

    public static double        autoRingCollectTimeAllowed = 1.5; // time allowed to let the single ring to get picked up

    // These constants define the desired driving/control characteristics
    // The can/should be tweaked to suit the specific robot drive train.
    public static final double     DRIVE_SPEED             = 0.85;     // Nominal speed for better accuracy.
    public static final double     TURN_SPEED              = 0.40;    // 0.4 for berber carpet. Check on mat too

    public static final double     HEADING_THRESHOLD       = 1.5;      // As tight as we can make it with an integer gyro
    public static final double     Kp_TURN                 = 0.02;   //0.025 to 0.0275 on mat seems to work
    public static final double     Ki_TURN                 = 0.0;   //0.0025 to 0.004 on a mat works. Battery voltage matters
    public static final double     Kd_TURN                 = 0.0;   //leave as 0
    public static final double     Kp_DRIVE                = 0.03;   //0.05 Larger is more responsive, but also less stable
    public static final double     Ki_DRIVE                = 0.000;   // 0.005 Larger is more responsive, but also less stable
    public static final double     Kd_DRIVE                = 0.0;   // Leave as 0 for now

    // Gyro constants and variables for PID steering

    private double                 globalAngle; // not used currently
    public double                  lasterror;
    public  double                 totalError;




    @Override
    public void runOpMode() {
        drivetrain.init(hardwareMap, true); // call the init method in the subsystem. THis saves space here
        // Gyro set-up
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();



        drivetrain.leftFront.setDirection(DcMotor.Direction.REVERSE);
        drivetrain.rightFront.setDirection(DcMotor.Direction.REVERSE);
        drivetrain.rightRear.setDirection(DcMotor.Direction.FORWARD);
        drivetrain.leftRear.setDirection(DcMotor.Direction.FORWARD);

        parameters.mode                = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled      = false;

        // Init gyro parameters then calibrate
        drivetrain.imu.initialize(parameters);

        // Ensure the robot it stationary, then reset the encoders and calibrate the gyro.
        // Encoder rest is handled in the Drivetrain init in Drivetrain class

        // Calibrate gyro

        telemetry.addData("Mode", "calibrating...");
        telemetry.update();

        // make sure the gyro is calibrated before continuing
        while (!isStopRequested() && !drivetrain.imu.isGyroCalibrated())  {
            sleep(50);
            idle();
        }

        telemetry.addData(">", "Robot Ready.");    //
        telemetry.update();

        telemetry.addData("Mode", "waiting for start");
        telemetry.addData("imu calib status", drivetrain.imu.getCalibrationStatus().toString());
        /** Wait for the game to begin */


        telemetry.update();

        /////////////////////////////////////////////////////////////////////////////////////////////
        waitForStart();
        ////////////////////////////////////////////////////////////////////////////////////////////



        drivetime.reset(); // reset because time starts when TF starts and time is up before we can call gyroDrive
        // Drive paths are initially all the same to get to the shooter location
        //gyroDrive(DRIVE_SPEED, 36.0, 0.0, 10);
        //gyroTurn(TURN_SPEED,90,10);
        //gyroDrive(DRIVE_SPEED,36,90,3);
        //gyroTurn(TURN_SPEED,180,3);
        //gyroDrive(DRIVE_SPEED,36,180,3);
        //gyroTurn(TURN_SPEED,-90,3);
        //gyroDrive(DRIVE_SPEED,36,-90,3);

        telemetry.addData("Path", "Complete");
        telemetry.update();
    }


    /**
     *  Method to drive on a fixed compass bearing (angle), based on encoder counts.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the desired position
     *  2) Driver stops the opmode running.
     *  3) Timeout time is reached - prevents robot from getting stuck
     *
     * @param speed      Target speed for forward motion.  Should allow for _/- variance for adjusting heading
     * @param distance   Distance (in inches) to move from current position.  Negative distance means move backwards.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     */
    public void gyroDrive ( double speed,
                            double distance,
                            double angle, double timeout) {

        int     newLeftFrontTarget;
        int     newRightFrontTarget;
        int     newLeftRearTarget;
        int     newRightRearTarget;
        int     moveCounts;
        double  max;
        double  error;
        double  steer;
        double  leftSpeed;
        double  rightSpeed;
        totalError = 0;
        lasterror = 0;
        telemetry.addData("gyroDrive Activated", "Complete");
        // Ensure that the opmode is still active
        // Use timeout in case robot gets stuck in mid path.
        // Also a way to keep integral term from winding up to bad.
        if (opModeIsActive() & drivetime.time() < timeout) {

            // Determine new target position in ticks/ counts then pass to motor controller
            moveCounts = (int)(distance *  Drivetrain.COUNTS_PER_INCH);
            newLeftFrontTarget = drivetrain.leftFront.getCurrentPosition() + moveCounts;
            newRightFrontTarget = drivetrain.rightFront.getCurrentPosition() + moveCounts;
            newLeftRearTarget = drivetrain.leftRear.getCurrentPosition() + moveCounts;
            newRightRearTarget = drivetrain.rightRear.getCurrentPosition() + moveCounts;
            // Set Target using the calculated umber of ticks/counts

            drivetrain.leftFront.setTargetPosition(newLeftFrontTarget);
            drivetrain.rightFront.setTargetPosition(newRightFrontTarget);
            drivetrain.leftRear.setTargetPosition(newLeftRearTarget);
            drivetrain.rightRear.setTargetPosition(newRightRearTarget);
            // Tell motor control to use encoders to go to target tick count.

            drivetrain.leftFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            drivetrain.rightFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            drivetrain.leftRear.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            drivetrain.rightRear.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // start motion.
            // Up to now this is all the same as a drive by encoder opmode.
            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            drivetrain.leftFront.setPower(speed);
            drivetrain.rightFront.setPower(speed);
            drivetrain.leftRear.setPower(speed);
            drivetrain.rightRear.setPower(speed);

            // keep looping while we are still active, and BOTH motors are running.
            // once one motor gets to the target number of ticks it is no longer "busy"
            // and isbusy in false causing the loop to end.
            while (opModeIsActive() &&
                    (drivetrain.leftFront.isBusy() && drivetrain.rightFront.isBusy())) {

                // adjust relative speed based on heading error.
                // Positive angle means drifting to the left so need to steer to the
                // right to get back on track.
                error = getError(angle);
                steer = getSteer(error, Kp_DRIVE, Ki_DRIVE, Kd_DRIVE);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;

                leftSpeed = speed + steer;
                rightSpeed = speed - steer;

                // Normalize speeds if either one exceeds +/- 1.0;
                max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
                if (max > 1.0)
                {
                    leftSpeed /= max;
                    rightSpeed /= max;
                }

                drivetrain.leftFront.setPower(leftSpeed);
                drivetrain.rightFront.setPower(rightSpeed);
                drivetrain.leftRear.setPower(leftSpeed);
                drivetrain.rightRear.setPower(rightSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d",      newLeftFrontTarget,  newRightFrontTarget);
                telemetry.addData("Actual",  "%7d:%7d",      drivetrain.leftFront.getCurrentPosition(),
                        drivetrain.rightFront.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  leftSpeed, rightSpeed);
                telemetry.update();


            }

            // Stop all motion;
            drivetrain.leftFront.setPower(0);
            drivetrain.rightFront.setPower(0);
            drivetrain.leftRear.setPower(0);
            drivetrain.rightRear.setPower(0);

            // Turn off RUN_TO_POSITION
            drivetrain.leftFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            drivetrain.rightFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            drivetrain.leftRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            drivetrain.rightRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        drivetime.reset(); // reset the timer for the next function call
    }


    /**
     *  Method to spin on central axis to point in a new direction.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the heading (angle)
     *  2) Driver stops the opmode running.
     *  3) Timeout time has elapsed - prevents getting stuck
     *
     * @param speed Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     * @param timeout max time allotted to complete each call to gyroTurn
     */
    public void gyroTurn (  double speed, double angle, double timeout) {
        totalError = 0;
        lasterror = 0;
        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && !onHeading(speed, angle, Kp_TURN, Ki_TURN, Kd_TURN) && drivetime.time() < timeout) {
            // Update telemetry & Allow time for other processes to run.
            //onHeading(speed, angle, P_TURN_COEFF);
            telemetry.update();
        }
       drivetime.reset(); // reset after we are done with the while loop
    }

    /**
     *  Method to obtain & hold a heading for a finite amount of time
     *  Move will stop once the requested time has elapsed
     *
     * @param speed      Desired speed of turn.
     * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     * @param holdTime   Length of time (in seconds) to hold the specified heading.
     */
    public void gyroHold( double speed, double angle, double holdTime) {

        ElapsedTime holdTimer = new ElapsedTime();

        // keep looping while we have time remaining.
        holdTimer.reset();
        while (opModeIsActive() && (holdTimer.time() < holdTime)) {
            // Update telemetry & Allow time for other processes to run.
            onHeading(speed, angle, Kp_TURN, Ki_TURN, Kd_TURN);
            telemetry.update();
        }

        // Stop all motion;
        drivetrain.leftFront.setPower(0);
        drivetrain.rightFront.setPower(0);
    }

    /**
     * Perform one cycle of closed loop heading control.
     *
     * @param speed     Desired speed of turn.
     * @param angle     Absolute Angle (in Degrees) relative to last gyro reset.
     *                  0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                  If a relative angle is required, add/subtract from current heading.
     *
     * @return
     */
    public boolean onHeading(double speed, double angle, double P_TURN_COEFF , double I_TURN_COEFF, double D_TURN_COEFF) {
        double   error ;
        double   steer ;
        boolean  onTarget = false ;
        double leftSpeed;
        double rightSpeed;

        // determine turn power based on +/- error
        error = getError(angle);

        if (Math.abs(error) <= HEADING_THRESHOLD) {
            steer = 0.0;
            leftSpeed  = 0.0;
            rightSpeed = 0.0;
            onTarget = true;
        }
        else {
            steer = getSteer(error, P_TURN_COEFF , I_TURN_COEFF, D_TURN_COEFF);
            rightSpeed  = -speed * steer;
            leftSpeed   = -rightSpeed;
        }

        // Send desired speeds to motors.
        drivetrain.leftFront.setPower(leftSpeed);
        drivetrain.rightFront.setPower(rightSpeed);
        drivetrain.leftRear.setPower(leftSpeed);
        drivetrain.rightRear.setPower(rightSpeed);

        // Display it for the driver.
        telemetry.addData("Target", "%5.2f", angle);
        telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);
        telemetry.update();

        return onTarget;
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
     * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     *          +ve error means the robot should turn LEFT (CCW) to reduce error.
     *
     */
    public double getError(double targetAngle) {

        double robotError;

        // calculate error in -179 to +180 range  (
        // instantiate an angles object from the IMU
        Orientation angles = drivetrain.imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        // pull out the first angle which is the Z axis for heading and use to calculate the error
        // Positive robot rotation is left so positive error means robot needs to turn right.
        robotError = angles.firstAngle - targetAngle; //lastAngles.firstAngle;
        telemetry.addData("Robot Error", robotError);
        telemetry.addData("Target Angle", targetAngle);
        while (robotError > 180)  robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
    }

    /**
     * returns desired steering force.  +/- 1 range.  +ve = steer left
     * @param error   Error angle in robot relative degrees
     * @param PCoeff  Proportional Gain Coefficient
     * @param ICoef Integration coefficient to apply to the area under the error-time curve
     * @param DCoef Derivative coefficient to apply to the area under the error-time curve
     * @return
     */
    public double getSteer(double error, double PCoeff, double ICoef, double DCoef) {
        double errorP; // combined proportional error Kp*error
        double errorI; // combined integral error Ki * cumulative error
        double errorD; // combined derivative error Kd*change in error
        double changeInError;

        changeInError = error - lasterror; // for the integral term only. Cumulative error tracking
        errorP = PCoeff * error;
        totalError = totalError  + error * PIDtimer.time();
        errorI = ICoef * totalError;
        errorD = DCoef * (changeInError)/PIDtimer.time();
        lasterror = error;
        PIDtimer.reset();

        //return Range.clip(error *(PCoeff+ICoef+DCoef), -1, 1);
        return Range.clip((errorP + errorI + errorD),-1,1);

    }



    /**
     * Initialize the TensorFlow Object Detection engine.
     */





    



}
