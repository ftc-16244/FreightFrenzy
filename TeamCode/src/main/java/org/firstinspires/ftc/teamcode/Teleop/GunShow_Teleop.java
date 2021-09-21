package org.firstinspires.ftc.teamcode.Teleop;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.teamcode.Auto.BasicGunShow;
import org.firstinspires.ftc.teamcode.Subsystems.Drivetrain;

@TeleOp(name="GunShow Teleop", group="Teleop")
@Disabled
public class GunShow_Teleop extends BasicGunShow {

    @Override
    public void runOpMode() {
        // All of the drivetrain details are in the subsystems package. This keep the clutter down.
        // Plus all drivetrain details can e reused for all opmodes and never recreated.
        Drivetrain drivetrain = new Drivetrain();
        // Add servos or other motors here as needed.

        // The "init" methods below are pointing back to the sybsystems
        // The drivetrain subsystem lets you pick between teleop or auto.
        // inTeleop = true and inTelop = false (aka autonomous).
        // If you get true/ false wrong the robot will not drive.

        // This line is required for each opmode.
        // Forgetting this lie will lead to a null exception and "missing hardware error"
        drivetrain.init(hardwareMap, true);


        drivetrain.leftFront.setDirection(DcMotor.Direction.REVERSE);
        drivetrain.rightFront.setDirection(DcMotor.Direction.REVERSE);
        drivetrain.rightRear.setDirection(DcMotor.Direction.REVERSE);
        drivetrain.leftRear.setDirection(DcMotor.Direction.REVERSE);


        waitForStart();
        if (isStopRequested()) return;
        while (opModeIsActive()) {
            double y = -gamepad1.left_stick_y; // Remember, this is reversed!
            double x = gamepad1.left_stick_x;
            double rx = gamepad1.right_stick_x;
            double lf, lr, rf, rr;

            lf = (y + x + rx); // forward + turn right + strafe right
            lr = (y - x + rx);
            rf = (y - x - rx);
            rr = (y + x - rx);

            if (Math.abs(lf) > 1 || Math.abs(lr) > 1 || Math.abs(rf) > 1 || Math.abs(rr) > 1) {
                double max = 0;
                max = Math.max(Math.abs(lf), Math.abs(lr));
                max = Math.max(Math.abs(rf), max);
                max = Math.max(Math.abs(rr), max);

                // scales output if y + x + rx >1
                lf /= max;
                lr /= max;
                rf /= max;
                rr /= max;

            }


            drivetrain.leftFront.setPower(lf);
            drivetrain.leftRear.setPower(lr);
            drivetrain.rightFront.setPower(rf);
            drivetrain.rightRear.setPower(rr);

            telemetry.addData("Left Stick Y-Fwd", "%5.2f", y);
            telemetry.addData("Right  Stick X-Turn", "%5.2f", x);
            telemetry.addData("Left Stick RX-Rotation", "%5.2f", rx);
            telemetry.update();


        }
    }

}