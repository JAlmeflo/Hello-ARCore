package se.wip.helloarcore;

import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.QuaternionEvaluator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;

public class AnimationNode extends Node {
    private static final String TAG = AnimationNode.class.getSimpleName();

    private static final float ANIMATION_MOVE_LENGTH = 1.0f;
    private static final long ANIMATION_DURATION = 500;
    private static final long ANIMATION_RESET_DURATION = 1500;

    @Nullable
    private ObjectAnimator animation = null;
    @Nullable
    private ObjectAnimator resetAnimation = null;

    private enum AnimationState {
        None,
        Running,
        Resetting,
        Done
    }
    private AnimationState animationState = AnimationState.None;

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);


        if (animation == null) {
            return;
        }

        switch (animationState) {
            case Running:
                if (!animation.isRunning()) {
                    resetAnimation.start();
                    animationState = AnimationState.Resetting;
                }
                break;
            case Resetting:
                if (!resetAnimation.isRunning()) {
                    animationState = AnimationState.Done;
                }
                break;
            case Done:
                break;
        }
    }



    public void startAnimation() {
        if (animation == null) {
            animation = createMoveAnimator();
            animation.setTarget(this);
        }

        if (resetAnimation == null) {
            resetAnimation = createResetMoveAnimator();
            resetAnimation.setTarget(this);
        }

        switch (animationState) {
            case None:
            case Done:
                animation.start();
                animationState = AnimationState.Running;
                break;
        }
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        if (resetAnimation != null) {
            resetAnimation.cancel();
            resetAnimation = null;
        }

        animationState = AnimationState.None;
    }

    private ObjectAnimator createMoveAnimator() {
        ObjectAnimator objectAnimation = new ObjectAnimator();
        objectAnimation.setObjectValues(Vector3.zero(), getMoveToVector3());

        // Give it the localPosition property.
        objectAnimation.setPropertyName("localPosition");

        objectAnimation.setEvaluator(new Vector3Evaluator());
        objectAnimation.setInterpolator(new DecelerateInterpolator());
        objectAnimation.setAutoCancel(true);
        objectAnimation.setDuration(ANIMATION_DURATION);

        return objectAnimation;
    }

    private ObjectAnimator createResetMoveAnimator() {

        ObjectAnimator objectAnimation = new ObjectAnimator();
        objectAnimation.setObjectValues(getMoveToVector3(), Vector3.zero());
        objectAnimation.setPropertyName("localPosition");
        objectAnimation.setEvaluator(new Vector3Evaluator());
        objectAnimation.setInterpolator(new LinearInterpolator());
        objectAnimation.setAutoCancel(true);
        objectAnimation.setDuration(ANIMATION_RESET_DURATION);

        return objectAnimation;
    }

    private Vector3 getMoveToVector3() {

        // Andy
        // (left, up, forward) -> (0, 0, -1) = back direction

        // Cannon
        // (forward, up, right) -> (-1, 0, 0) = back direction

        Vector3 dir = new Vector3(-1,0,0);
        // Make sure the direction vector is normalized
        dir = dir.normalized();
        // Rotate the direction using the localRotation
        Quaternion rotation = getLocalRotation();
        Vector3 actualDir = Quaternion.rotateVector(rotation, dir);

        Vector3 newPos = new Vector3();
        newPos.x = actualDir.x * ANIMATION_MOVE_LENGTH;
        newPos.y = actualDir.y * ANIMATION_MOVE_LENGTH;
        newPos.z = actualDir.z * ANIMATION_MOVE_LENGTH;

        return newPos;
    }

    /** Returns an ObjectAnimator that makes this node rotate. */
    private ObjectAnimator createRotationAnimator(boolean clockwise, float axisTiltDeg) {
        // Node's setLocalRotation method accepts Quaternions as parameters.
        // First, set up orientations that will animate a circle.
        Quaternion[] orientations = new Quaternion[4];
        // Rotation to apply first, to tilt its axis.
        Quaternion baseOrientation = Quaternion.axisAngle(new Vector3(1.0f, 0f, 0.0f), axisTiltDeg);
        for (int i = 0; i < orientations.length; i++) {
            float angle = i * 360 / (orientations.length - 1);
            if (clockwise) {
                angle = 360 - angle;
            }
            Quaternion orientation = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), angle);
            orientations[i] = Quaternion.multiply(baseOrientation, orientation);
        }

        ObjectAnimator rotationAnimation = new ObjectAnimator();
        // Cast to Object[] to make sure the varargs overload is called.
        rotationAnimation.setObjectValues((Object[]) orientations);

        // Next, give it the localRotation property.
        rotationAnimation.setPropertyName("localRotation");

        // Use Sceneform's QuaternionEvaluator.
        rotationAnimation.setEvaluator(new QuaternionEvaluator());

        //  Allow orbitAnimation to repeat forever
        rotationAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimation.setRepeatMode(ObjectAnimator.RESTART);
        rotationAnimation.setInterpolator(new LinearInterpolator());
        rotationAnimation.setAutoCancel(true);
        rotationAnimation.setDuration(1000);

        return rotationAnimation;
    }
}
