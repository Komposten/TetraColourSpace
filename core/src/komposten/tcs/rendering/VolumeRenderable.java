/*
 * Copyright 2019 Jakob Hjelm
 * 
 * This file is part of TetraColourSpace.
 * 
 * TetraColourSpace is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package komposten.tcs.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.data.Volume;
import komposten.tcs.util.TCSUtils;

public class VolumeRenderable implements Disposable
{
	private static Vector3 calcVector = new Vector3();
	private static Vector3 calcVector2 = new Vector3();
	
	private Mesh mesh;
	private Renderable renderable;
	
	public VolumeRenderable(Volume data, Environment environment)
	{
		this.mesh = createMesh(data);
		this.renderable = createRenderable(mesh, data.getColour(), environment);
	}
	
	
	private Mesh createMesh(Volume data)
	{
		MeshBuilder meshBuilder = new MeshBuilder();
		
		if (data.getCoordinates().length >= 9) //3 vertices
		{
			return createPolygonMesh(data, meshBuilder);
		}
		else if (data.getCoordinates().length == 6) //2 vertices
		{
			return createLineMesh(data, meshBuilder);
		}
		else
		{
			throw new IllegalArgumentException("A volume must contain at least 2 data points!");
		}
	}


	private Mesh createPolygonMesh(Volume data, MeshBuilder meshBuilder)
	{
		double[] volumeCoords = data.getCoordinates();
		int vertexCount = volumeCoords.length / 3;
		
		Vector3 vector1 = new Vector3();
		Vector3 vector2 = new Vector3();
		Vector3 vector3 = new Vector3();
		Vector3 vector4 = new Vector3();
		Vector3 normal = new Vector3();
		
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
		for (int[] face : data.getFaces())
		{
			int vertex1Index = face[0]*3;
			int vertex2Index = face[1]*3;
			int vertex3Index = face[2]*3;
			
			vector1.set((float) volumeCoords[vertex1Index],
					(float) volumeCoords[vertex1Index + 1],
					(float) volumeCoords[vertex1Index + 2]);
			vector2.set((float) volumeCoords[vertex2Index],
					(float) volumeCoords[vertex2Index + 1],
					(float) volumeCoords[vertex2Index + 2]);
			vector3.set((float) volumeCoords[vertex3Index],
					(float) volumeCoords[vertex3Index + 1],
					(float) volumeCoords[vertex3Index + 2]);
			
			if (vertexCount <= 3)
			{
				getFaceNormal(vector1, vector2, vector3, normal);
			}
			else
			{
				// This code ensures that the normal is pointing outwards.
				// Basic idea: 
				// 1) Find a vertex not in the current face.
				// 2) Check if adding or subtracting the normal takes us away from said vertex.
				
				findFourthVertex(volumeCoords, vertex1Index, vertex2Index, vertex3Index, vector4);
				getFaceNormal(vector1, vector2, vector3, vector4, normal);
			}
			
			short vertex1 = meshBuilder.vertex(vector1, normal, Color.WHITE, null);
			short vertex2 = meshBuilder.vertex(vector2, normal, Color.WHITE, null);
			short vertex3 = meshBuilder.vertex(vector3, normal, Color.WHITE, null);
			meshBuilder.triangle(vertex1, vertex2, vertex3);
		}
		
		return meshBuilder.end();
	}

	
	/**
	 * Calculates a normal for the face created by the three positions.
	 * @param normal A <code>Vector3</code> to store the calculated normal in.
	 */
	private void getFaceNormal(Vector3 pos1, Vector3 pos2, Vector3 pos3, Vector3 normal)
	{
		calcVector.set(pos2).sub(pos1);
		calcVector2.set(pos3).sub(pos1);
		normal.x = calcVector.y*calcVector2.z - calcVector.z*calcVector2.y;
		normal.y = calcVector.z*calcVector2.x - calcVector.x*calcVector2.z;
		normal.z = calcVector.x*calcVector2.y - calcVector.y*calcVector2.x;
		normal.nor();
	}
	
	
	/**
	 * Calculates a normal for the face created by the first three positions.
	 * The fourth position is used to ensure that the normal points outwards (i.e.
	 * away from <code>pos4</code>).
	 * @param normal A <code>Vector3</code> to store the calculated normal in.
	 */
	private void getFaceNormal(Vector3 pos1, Vector3 pos2, Vector3 pos3, Vector3 pos4, Vector3 normal)
	{
		getFaceNormal(pos1, pos2, pos3, normal);
		
		float length1 = calcVector.set(pos1).add(normal).sub(pos4).len2();
		float length2 = calcVector.set(pos1).sub(normal).sub(pos4).len2();
		
		if (length1 < length2)
			normal.scl(-1);
	}


	private void findFourthVertex(double[] volumeCoords, int vertex1Index, int vertex2Index,
			int vertex3Index, Vector3 output)
	{
		int vertex4Index = (vertex1Index + 3) % volumeCoords.length;
		while (vertex4Index == vertex2Index || vertex4Index == vertex3Index)
			vertex4Index = (vertex4Index + 3) % volumeCoords.length;

		output.set((float) volumeCoords[vertex4Index],
			(float) volumeCoords[vertex4Index + 1],
			(float) volumeCoords[vertex4Index + 2]);
	}


	private Mesh createLineMesh(Volume data, MeshBuilder meshBuilder)
	{
		double[] volumeCoords = data.getCoordinates();
		Vector3 vector1 = new Vector3();
		Vector3 vector2 = new Vector3();
		
		vector1.set((float) volumeCoords[0],
				(float) volumeCoords[1],
				(float) volumeCoords[2]);
		vector2.set((float) volumeCoords[3],
				(float) volumeCoords[4],
				(float) volumeCoords[5]);
		
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
		meshBuilder.line(vector1, vector2);
		
		return meshBuilder.end();
	}


	private Renderable createRenderable(Mesh mesh, Color colour, Environment environment)
	{
		Renderable r = new Renderable();
		if (mesh.getNumVertices() > 2)
		{
			r.meshPart.set("volume_polygon", mesh, 0, mesh.getNumVertices(), GL20.GL_TRIANGLES);
			r.material = TCSUtils.getMaterialForColour(colour).copy();
			r.material.set(new BlendingAttribute(0.5f));
			r.environment = environment;
		}
		else
		{
			r.meshPart.set("volume_line", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
			r.material = TCSUtils.getMaterialForColour(colour).copy();
			r.environment = environment;
		}
		
		return r;
	}


	public void render(ModelBatch batch)
	{
		batch.render(renderable);
	}


	@Override
	public void dispose()
	{
		mesh.dispose();
	}
}
