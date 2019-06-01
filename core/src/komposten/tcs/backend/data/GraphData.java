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
package komposten.tcs.backend.data;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.github.quickhull3d.QuickHull3D;

public class GraphData
{
	private List<PointGroup> dataGroups;
	private List<Volume> dataVolumes;
	
	public GraphData()
	{
		dataGroups = new ArrayList<>();
		dataVolumes = new ArrayList<>();
	}
	
	
	public List<PointGroup> getDataGroups()
	{
		return dataGroups;
	}
	
	
	public List<Volume> getDataVolumes()
	{
		return dataVolumes;
	}
	
	
	public int addGroup(String name, Shape shape, float size)
	{
		PointGroup group = new PointGroup(name, shape, size);
		dataGroups.add(group);
		
		return dataGroups.size()-1;
	}
	
	
	public void addPoint(String name, Vector3 coordinates, Vector3 metrics, Color colour,
			int groupIndex)
	{
		PointGroup group = dataGroups.get(groupIndex);
		Point point = new Point(name, coordinates, metrics, colour, group);
		group.getPoints().add(point);
	}
	
	
	public void addVolume(double[] coordinates, Color colour)
	{
		int[][] faces = null;
		if (coordinates.length >= 12)
		{
			QuickHull3D quickHull = new QuickHull3D();
			quickHull.build(coordinates);
			
			coordinates = new double[quickHull.getNumVertices()*3];
			quickHull.getVertices(coordinates);
			
			faces = quickHull.getFaces();
		}
		else if (coordinates.length == 9)
		{
			faces = new int[1][];
			faces[0] = new int[] { 0, 1, 2 };
		}
		else
		{
			faces = new int[0][];
		}
		
		dataVolumes.add(new Volume(coordinates, faces, colour));
	}
}
