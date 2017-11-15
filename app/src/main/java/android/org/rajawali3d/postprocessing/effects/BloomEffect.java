/**
 * Copyright 2013 Dennis Ippel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */package android.org.rajawali3d.postprocessing.effects;

import android.org.rajawali3d.cameras.Camera;
import android.org.rajawali3d.postprocessing.APostProcessingEffect;
import android.org.rajawali3d.postprocessing.passes.BlendPass;
import android.org.rajawali3d.postprocessing.passes.BlendPass.BlendMode;
import android.org.rajawali3d.postprocessing.passes.BlurPass.Direction;
import android.org.rajawali3d.postprocessing.passes.BlurPass;
import android.org.rajawali3d.postprocessing.passes.ColorThresholdPass;
import android.org.rajawali3d.postprocessing.passes.CopyToNewRenderTargetPass;
import android.org.rajawali3d.postprocessing.passes.RenderPass;
import android.org.rajawali3d.renderer.RajawaliRenderer;
import android.org.rajawali3d.scene.RajawaliScene;

public class BloomEffect extends APostProcessingEffect {
	private RajawaliScene mScene;
	private Camera mCamera;
	private int mWidth;
	private int mHeight;
	private int mLowerThreshold;
	private int mUpperThreshold;
	private BlendMode mBlendMode;
	
	/**
	 * Bloom or glow is used to amplify light in a scene. It produces light bleeding. Bright light will extend
	 * to other parts of the scene. The colors that will bleed can be controlled by specifying the lower
	 * and upper threshold colors.
	 * 
	 * @param scene
	 * @param camera
	 * @param width
	 * @param height
	 * @param lowerThreshold
	 * @param upperThreshold
	 * @param blendMode
	 */
	public BloomEffect(RajawaliScene scene, Camera camera, int width, int height, int lowerThreshold, int upperThreshold, BlendMode blendMode) {
		super();
		mScene = scene;
		mCamera = camera;
		mWidth = width;
		mHeight = height;
		mLowerThreshold = lowerThreshold;
		mUpperThreshold = upperThreshold;
		mBlendMode = blendMode;
	}
	
	public void initialize(RajawaliRenderer renderer)
	{
		addPass(new ColorThresholdPass(mLowerThreshold, mUpperThreshold));
		addPass(new BlurPass(Direction.HORIZONTAL, 6, mWidth, mHeight));
		addPass(new BlurPass(Direction.VERTICAL, 6, mWidth, mHeight));
		CopyToNewRenderTargetPass copyPass = new CopyToNewRenderTargetPass("bloomPassTarget", renderer, mWidth, mHeight);
		addPass(copyPass);
		addPass(new RenderPass(mScene, mCamera, mScene.getBackgroundColor()));
		addPass(new BlendPass(mBlendMode, copyPass.getRenderTarget().getTexture()));
	}
}
