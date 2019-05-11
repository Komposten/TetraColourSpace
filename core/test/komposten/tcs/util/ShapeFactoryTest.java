package komposten.tcs.util;

import static komposten.tcs.util.test.ArrayAssert.assertThat;
import static komposten.tcs.util.test.Vector3Assert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.backend.Style;
import komposten.tcs.util.test.GdxTest;

class ShapeFactoryTest extends GdxTest
{
	private static ModelBuilder modelBuilder;
	private static MeshBuilder meshBuilder;


	@BeforeAll
	static void init()
	{
		modelBuilder = new ModelBuilder();
		meshBuilder = new MeshBuilder();
	}
	

	@Test
	void createSphere_primitive()
	{
		int primitive = GL20.GL_TRIANGLES;
		Model model = ShapeFactory.createSphere(modelBuilder, 1, primitive, 10);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);

		primitive = GL20.GL_LINES;
		model = ShapeFactory.createSphere(modelBuilder, 1, primitive, 10);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);
	}
	

	@Test
	void createSphere_diameter()
	{
		assertSphereDiameter(1);
		assertSphereDiameter(0.5f);
	}


	private void assertSphereDiameter(float diameter)
	{
		Vector3 temp = new Vector3();
		Model model = ShapeFactory.createSphere(modelBuilder, diameter, GL20.GL_TRIANGLES, 10);
		assertThat(model.meshes).hasSize(1);
		Mesh mesh = model.meshes.first();
		forEachVertex(mesh, v -> assertThat(temp.set(v[0], v[1], v[2])).hasMagnitude(diameter/2));
	}
	
	
	@Test
	void createBox_primitive()
	{
		int primitive = GL20.GL_TRIANGLES;
		Model model = ShapeFactory.createBox(modelBuilder, 1, primitive);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);
		
		primitive = GL20.GL_LINES;
		model = ShapeFactory.createBox(modelBuilder, 1, primitive);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);
	}
	
	
	@Test
	void createBox_size()
	{
		assertBoxSize(1);
		assertBoxSize(0.5f);
	}


	private void assertBoxSize(float diameter)
	{
		Model model = ShapeFactory.createBox(modelBuilder, diameter, GL20.GL_TRIANGLES);
		
		assertThat(model.meshes).hasSize(1);
		
		Mesh mesh = model.meshes.first();
		
		assertThat(mesh.getNumVertices()).isEqualTo(24);
		
		//Check that the vertices are positioned diameter/2 from origin along all axes.
		List<Vector3> vertexCoords = new ArrayList<>(mesh.getNumVertices());
		forEachVertex(mesh, v -> {
			assertThat(Math.abs(v[0])).isEqualTo(diameter/2, within(MathUtils.FLOAT_ROUNDING_ERROR));
			assertThat(Math.abs(v[1])).isEqualTo(diameter/2, within(MathUtils.FLOAT_ROUNDING_ERROR));
			assertThat(Math.abs(v[2])).isEqualTo(diameter/2, within(MathUtils.FLOAT_ROUNDING_ERROR));
			vertexCoords.add(new Vector3(v[0], v[1], v[2]));
		});
		
		//Check that every corner has 3 overlapping vertices.
		vertexCoords.sort((o1, o2) ->
		{
			if (!MathUtils.isEqual(o1.x, o2.x, 0.001f))
				return Float.compare(o1.x, o2.x);
			if (!MathUtils.isEqual(o1.y, o2.y, 0.001f))
				return Float.compare(o1.y, o2.y);
			if (!MathUtils.isEqual(o1.z, o2.z, 0.001f))
				return Float.compare(o1.z, o2.z);
			return 0;
		});
		
		for (int i = 0; i < vertexCoords.size()-1; i++)
		{
			if ((i % 3) != 2)
				assertThat(vertexCoords.get(i)).as("should overlap").isEqualTo(vertexCoords.get(i+1));
			else
				assertThat(vertexCoords.get(i)).as("should not overlap").isNotEqualTo(vertexCoords.get(i+1));
		}
	}
	
	
	@Test
	void createTetrahedron_primitive()
	{
		int primitive = GL20.GL_TRIANGLES;
		Model model = ShapeFactory.createTetrahedron(modelBuilder, 1.0f, primitive);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);

		primitive = GL20.GL_LINES;
		model = ShapeFactory.createTetrahedron(modelBuilder, 1, primitive);
		assertThat(model.meshParts).hasSize(1);
		assertThat(model.meshParts.first().primitiveType).isEqualTo(primitive);
	}
	
	
	@Test
	void createTetrahedron_size()
	{
		//CURRENT Maybe the Tetrahedron class can be used here to determine the 
		//					positions of the vertices?
		assertTetrahedronSize(1.0f);
		assertTetrahedronSize(0.5f);
	}
	
	
	private void assertTetrahedronSize(float size)
	{
		Tetrahedron tetrahedron = new Tetrahedron(size);
		
		Model model = ShapeFactory.createTetrahedron(modelBuilder, size, GL20.GL_TRIANGLES);
		assertThat(model.meshes).hasSize(1);
		
		Mesh mesh = model.meshes.first();
		
		assertThat(mesh.getNumVertices()).isEqualTo(12);
		
		//Check that the vertices are positioned diameter/2 from origin along all axes.
		List<Vector3> vertexCoords = new ArrayList<>(mesh.getNumVertices());
		forEachVertex(mesh, v -> vertexCoords.add(new Vector3(v[0], v[1], v[2])));
		
		//Check that every corner has 3 overlapping vertices.
		vertexCoords.sort((o1, o2) ->
		{
			if (!MathUtils.isEqual(o1.x, o2.x, 0.001f))
				return Float.compare(o1.x, o2.x);
			if (!MathUtils.isEqual(o1.y, o2.y, 0.001f))
				return Float.compare(o1.y, o2.y);
			if (!MathUtils.isEqual(o1.z, o2.z, 0.001f))
				return Float.compare(o1.z, o2.z);
			return 0;
		});

		for (int i = 0; i < 3; i++)
			assertThat(vertexCoords.get(i)).as("short vertex").isEqualTo(tetrahedron.shortPos);
		for (int i = 3; i < 6; i++)
			assertThat(vertexCoords.get(i)).as("medium vertex").isEqualTo(tetrahedron.mediumPos);
		for (int i = 6; i < 9; i++)
			assertThat(vertexCoords.get(i)).as("uv vertex").isEqualTo(tetrahedron.uvPos);
		for (int i = 9; i < 12; i++)
			assertThat(vertexCoords.get(i)).as("long vertex").isEqualTo(tetrahedron.longPos);
	}
	
	
	@Test
	void createTetrahedronSideMeshes()
	{
		Mesh[] meshes = ShapeFactory.createTetrahedronSideMeshes(1.0f, meshBuilder, null);
		
		assertThat(meshes.length).isEqualTo(4);
		
		for (Mesh mesh : meshes)
			forEachVertex(mesh, (v) ->
			{
				SoftAssertions softly = new SoftAssertions();
				softly.assertThat(v[3]).isEqualTo(1f, within(0.001f));
				softly.assertThat(v[4]).isEqualTo(1f, within(0.001f));
				softly.assertThat(v[5]).isEqualTo(1f, within(0.001f));
				softly.assertThat(v[6]).isEqualTo(1f, within(0.001f));
				softly.assertAll();
			});

		Style style = new Style();
		meshes = ShapeFactory.createTetrahedronSideMeshes(1.0f, meshBuilder, style);
		
		for (Mesh mesh : meshes)
			forEachVertex(mesh, (v) ->
			{
				Color colour = new Color(v[3], v[4], v[5], v[6]);
				assertThat(colour.toString()).isNotEqualTo("ffffffff");
			});
	}
	
	
	private void forEachVertex(Mesh mesh, Consumer<float[]> consumer)
	{
		int stride = mesh.getVertexSize() / 4;
		float[] vertices = mesh.getVertices(new float[mesh.getNumVertices() * stride]);
		
		float[] vertex = new float[stride];
		
		for (int i = 0; i < mesh.getNumVertices(); i++)
		{
			for (int j = 0; j < stride; j++)
			{
				vertex[j] = vertices[i*stride+j];
			}
			
			consumer.accept(vertex);
		}
	}
}
