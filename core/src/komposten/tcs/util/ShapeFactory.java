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
package komposten.tcs.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;

public class ShapeFactory
{
	private ShapeFactory() {}
	

	public static Model createSphere(ModelBuilder modelBuilder, float diameter, int primitiveType, int segments)
	{
		return modelBuilder.createSphere(
				diameter, diameter, diameter, segments, segments,
				primitiveType, new Material(), Usage.Position | Usage.Normal | Usage.ColorUnpacked);
	}
	
	
	public static Model createBox(ModelBuilder modelBuilder, float size, int primitiveType)
	{
		return modelBuilder.createBox(size, size, size, primitiveType,
				new Material(), Usage.Position | Usage.Normal | Usage.ColorUnpacked);
	}
	
	
	public static Model createTetrahedron(ModelBuilder modelBuilder, float size, int primitiveType)
	{
		Mesh mesh = createTetrahedronMesh(size, new MeshBuilder(), null);
		
		modelBuilder.begin();
		modelBuilder.part("tetrahedron", mesh, primitiveType, new Material());
		return modelBuilder.end();
	}


	/**
	 * @param size
	 * @param meshBuilder
	 * @param style A style with the colours to use for the four corners.
	 *          <code>WL_LONG, WL_MEDIUM, WL_SHORT</code>, and <code>WL_UV</code> are used.
	 * @return A single mesh containing a tetrahedron.
	 */
	public static Mesh createTetrahedronMesh(float size, MeshBuilder meshBuilder, Style style)
	{
		Mesh[] sides = createTetrahedronSideMeshes(size, meshBuilder, style);
		
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
		for (Mesh side : sides)
		{
			meshBuilder.addMesh(side);
			side.dispose();
		}
		
		return meshBuilder.end();
	}
	

	/**
	 * @param size
	 * @param meshBuilder
	 * @param style A style with the colours to use for the four corners.
	 *          <code>WL_LONG, WL_MEDIUM, WL_SHORT</code>, and <code>WL_UV</code> are used.
	 * @return An array containing one mesh for each of the four sides in the tetrahedron.
	 */
	@SuppressWarnings("null")
	public static Mesh[] createTetrahedronSideMeshes(float size, MeshBuilder meshBuilder, Style style)
	{
		Tetrahedron tetrahedron = new Tetrahedron(size);
		Mesh[] meshes = new Mesh[4];
		int meshAttributes = Usage.Position | Usage.Normal | Usage.ColorUnpacked;
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);
		
		Vector3 longPos = tetrahedron.longPos;
		Vector3 mediumPos = tetrahedron.mediumPos;
		Vector3 shortPos = tetrahedron.shortPos;
		Vector3 uvPos = tetrahedron.uvPos;
		
		boolean hasStyle = (style != null);
		Color longColourActive = (hasStyle ? style.get(Colour.WL_LONG) : Color.WHITE);
		Color mediumColourActive = (hasStyle ? style.get(Colour.WL_MEDIUM) : Color.WHITE);
		Color shortColourActive = (hasStyle ? style.get(Colour.WL_SHORT) : Color.WHITE);
		Color uvColourActive = (hasStyle ? style.get(Colour.WL_UV) : Color.WHITE);

		Vector3 normal = mediumPos.cpy().add(longPos).add(shortPos);
		short corner1 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		short corner2 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		short corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		meshes[0] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);
		
		normal = longPos.cpy().add(mediumPos).add(uvPos);
		corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		meshes[1] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);

		normal = longPos.cpy().add(uvPos).add(shortPos);
		corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		meshes[2] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);

		normal = uvPos.cpy().add(mediumPos).add(shortPos);
		corner1 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		meshes[3] = meshBuilder.end();

		return meshes;
	}
}
