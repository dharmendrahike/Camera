/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package android.org.rajawali3d.renderer.pip;

import android.content.Context;
import android.org.rajawali3d.renderer.RajawaliRenderer;
import android.view.MotionEvent;

import android.org.rajawali3d.cameras.Camera;
import android.org.rajawali3d.scene.RajawaliScene;

/**
 * This class is intended to be used with <code>RajawaliPipRenderer</code>.
 */
public abstract class SubRenderer {
    private static final String TAG = "SubRenderer";
    private RajawaliScene scene;
    private RajawaliRenderer renderer;

    public SubRenderer(RajawaliRenderer renderer) {
        scene = new RajawaliScene(renderer);
        renderer.addScene(scene);
        this.renderer = renderer;
    }

    public RajawaliScene getCurrentScene() {
        return scene;
    }

    public Camera getCurrentCamera() {
        return scene.getCamera();
    }

    protected Context getContext() {
        return renderer.getContext();
    }

    public void doRender() {
        onRender();
    }

    protected abstract void onRender();

    public abstract void initScene();

    public void onTouchEvent(MotionEvent event) {
    }
}
