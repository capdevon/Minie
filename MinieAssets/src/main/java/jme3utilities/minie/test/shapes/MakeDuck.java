/*
 Copyright (c) 2019-2023, Stephen Gold
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
package jme3utilities.minie.test.shapes;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.BinLoader;
import com.jme3.scene.plugins.gltf.GltfLoader;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.texture.plugins.AWTLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import vhacd.VHACDParameters;

/**
 * A console application to generate the collision-shape asset "duck.j3o".
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MakeDuck {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MakeDuck.class.getName());
    /**
     * filesystem path to the asset directory/folder for output
     */
    final private static String assetDirPath
            = "../MinieExamples/src/main/resources";
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MakeDuck() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeDuck application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        NativeLibraryLoader.loadNativeLibrary("bulletjme", true);

        // Mute the chatty loggers found in some imported packages.
        Heart.setLoggingLevels(Level.WARNING);

        // Log the working directory.
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));

        // Generate the collision shape.
        makeDuck();
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a collision shape for a toy duck.
     */
    private static void makeDuck() {
        AssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLoader(AWTLoader.class, "png");
        assetManager.registerLoader(BinLoader.class, "bin");
        assetManager.registerLoader(GltfLoader.class, "gltf");
        assetManager.registerLoader(J3MLoader.class, "j3m", "j3md");
        assetManager.registerLocator(null, ClasspathLocator.class);
        /*
         * Import the Duck model (by Sony Computer Entertainment Inc.)
         * from src/main/resources:
         */
        Spatial cgmRoot = assetManager.loadModel("Models/Duck/Duck.gltf");
        Spatial parent = ((Node) cgmRoot).getChild(0);
        parent.setLocalTransform(Transform.IDENTITY);
        Spatial geom = ((Node) parent).getChild(0);

        // Translate and uniformly scale the model to fit inside a 2x2x2 cube.
        Vector3f[] minMax = MySpatial.findMinMaxCoords(geom);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1], null);
        Vector3f offset = center.negate();
        geom.setLocalTranslation(offset);

        Vector3f extents = minMax[1].subtract(minMax[0]);
        float radius = MyMath.max(extents.x, extents.y, extents.z) / 2f;
        parent.setLocalScale(1f / radius);

        // Generate a CollisionShape to approximate the Mesh.
        VHACDParameters parameters = new VHACDParameters();
        parameters.setMaxVerticesPerHull(99);
        parameters.setVoxelResolution(900_000);
        CompoundCollisionShape shape
                = ShapeUtils.createVhacdShape(cgmRoot, parameters, "MakeDuck");

        // Write the shape to the asset file.
        String assetPath = "CollisionShapes/duck.j3o";
        String writeFilePath = String.format("%s/%s", assetDirPath, assetPath);
        Heart.writeJ3O(writeFilePath, shape);
    }
}
