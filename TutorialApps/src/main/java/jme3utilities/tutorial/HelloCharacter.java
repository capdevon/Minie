/*
 Copyright (c) 2020-2025 Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.tutorial;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.Box2dShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.debug.DebugInitListener;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import jme3utilities.MeshNormals;

/**
 * A simple example of character physics.
 * <p>
 * Builds upon HelloCustomDebug.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloCharacter
        extends SimpleApplication
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * character being tested
     */
    private static PhysicsCharacter character;
    /**
     * space for physics simulation
     */
    private static PhysicsSpace physicsSpace;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the HelloCharacter application.
     * <p>
     * This no-arg constructor was made explicit to avoid javadoc warnings from
     * JDK 18+.
     */
    public HelloCharacter() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloCharacter application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloCharacter application = new HelloCharacter();

        // Enable gamma correction for accurate lighting:
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setGammaCorrection(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize the application. Invoked once.
     */
    @Override
    public void simpleInitApp() {
        physicsSpace = configurePhysics();

        // Create a character with a capsule shape and add it to the space:
        float capsuleRadius = 0.5f;
        float capsuleHeight = 1f;
        CapsuleCollisionShape shape
                = new CapsuleCollisionShape(capsuleRadius, capsuleHeight);
        float stepHeight = 0.01f;
        character = new PhysicsCharacter(shape, stepHeight);
        physicsSpace.addCollisionObject(character);

        // Add a square to represent the ground.
        float halfExtent = 4f;
        float y = -2f;
        PhysicsRigidBody ground = addSquare(halfExtent, y);

        // Customize the debug visualization of both physics objects:
        Material redMaterial = createLitMaterial(1f, 0f, 0f);
        character.setDebugMaterial(redMaterial);
        character.setDebugMeshNormals(MeshNormals.Smooth);
        character.setDebugMeshResolution(DebugShapeFactory.highResolution);

        Material greenMaterial = createLitMaterial(0f, 0.5f, 0f);
        ground.setDebugMaterial(greenMaterial);
        ground.setDebugMeshNormals(MeshNormals.Facet);

        // BulletAppState drives the physics simulation...
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just before each simulation step.
     *
     * @param space the space that's about to be stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        // If the character is touching the ground, cause it to jump.
        if (character.onGround()) {
            character.jump();
        }
    }

    /**
     * Callback from Bullet, invoked just after each simulation step.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the duration of the simulation step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        // do nothing
    }
    // *************************************************************************
    // private methods

    /**
     * Add lighting and shadows to the specified scene and set the background
     * color.
     *
     * @param scene the scene to augment (not null)
     */
    private void addLighting(Spatial scene) {
        ColorRGBA ambientColor = new ColorRGBA(0.03f, 0.03f, 0.03f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        scene.addLight(ambient);
        ambient.setName("ambient");

        ColorRGBA directColor = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        Vector3f direction = new Vector3f(-7f, -3f, -5f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction, directColor);
        scene.addLight(sun);
        sun.setName("sun");

        // Render shadows based on the directional light.
        viewPort.clearProcessors();
        int shadowMapSize = 2_048; // in pixels
        int numSplits = 3;
        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(
                        assetManager, shadowMapSize, numSplits);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        dlsr.setEdgesThickness(5);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.4f);
        viewPort.addProcessor(dlsr);

        // Set the viewport's background color to light blue.
        ColorRGBA skyColor = new ColorRGBA(0.1f, 0.2f, 0.4f, 1f);
        viewPort.setBackgroundColor(skyColor);
    }

    /**
     * Add a horizontal square body to the space.
     *
     * @param halfExtent half of the desired side length
     * @param y the desired elevation (in physics-space coordinates)
     * @return the new body (not null)
     */
    private PhysicsRigidBody addSquare(float halfExtent, float y) {
        // Construct a static rigid body with a square shape.
        Box2dShape shape = new Box2dShape(halfExtent);
        PhysicsRigidBody result
                = new PhysicsRigidBody(shape, PhysicsBody.massForStatic);

        physicsSpace.addCollisionObject(result);

        // Rotate it 90 degrees to a horizontal orientation.
        Quaternion rotate90
                = new Quaternion().fromAngles(-FastMath.HALF_PI, 0f, 0f);
        result.setPhysicsRotation(rotate90);

        // Translate it to the desired elevation.
        result.setPhysicsLocation(new Vector3f(0f, y, 0f));

        return result;
    }

    /**
     * Configure physics during startup.
     *
     * @return a new object
     */
    private PhysicsSpace configurePhysics() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // Enable debug visualization to reveal what occurs in physics space.
        bulletAppState.setDebugEnabled(true);

        // Add lighting and shadows to the debug scene.
        bulletAppState.setDebugInitListener(new DebugInitListener() {
            @Override
            public void bulletDebugInit(Node physicsDebugRootNode) {
                addLighting(physicsDebugRootNode);
            }
        });
        bulletAppState.setDebugShadowMode(
                RenderQueue.ShadowMode.CastAndReceive);

        PhysicsSpace result = bulletAppState.getPhysicsSpace();

        // To enable the callbacks, register the application as a tick listener.
        result.addTickListener(this);

        return result;
    }

    /**
     * Create a single-sided lit material with the specified reflectivities.
     *
     * @param red the desired reflectivity for red light (&ge;0, &le;1)
     * @param green the desired reflectivity for green light (&ge;0, &le;1)
     * @param blue the desired reflectivity for blue light (&ge;0, &le;1)
     * @return a new object
     */
    private Material createLitMaterial(float red, float green, float blue) {
        Material result = new Material(assetManager, Materials.LIGHTING);
        result.setBoolean("UseMaterialColors", true);

        float opacity = 1f;
        result.setColor("Ambient", new ColorRGBA(red, green, blue, opacity));
        result.setColor("Diffuse", new ColorRGBA(red, green, blue, opacity));

        return result;
    }
}
