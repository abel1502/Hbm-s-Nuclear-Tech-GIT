package com.hbm.render.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.TextureCoordinate;
import net.minecraftforge.client.model.obj.Vertex;

import com.hbm.util.Clock;

@Deprecated
public class HbmModelObject implements IModelCustom {
    private static Pattern vertexPattern = Pattern.compile("(v( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *\\n)|(v( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *$)");
    private static Pattern vertexNormalPattern = Pattern.compile("(vn( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *\\n)|(vn( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *$)");
    private static Pattern textureCoordinatePattern = Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+(\\.\\d+)?){2,3} *$)");
    private static Pattern face_V_VT_VN_Pattern = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
    private static Pattern face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
    private static Pattern face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
    private static Pattern face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");
    private static Pattern groupObjectPattern = Pattern.compile("([go]( [\\w\\d\\.]+) *\\n)|([go]( [\\w\\d\\.]+) *$)");

    private static Matcher vertexMatcher, vertexNormalMatcher, textureCoordinateMatcher;
    private static Matcher face_V_VT_VN_Matcher, face_V_VT_Matcher, face_V_VN_Matcher, face_V_Matcher;
    private static Matcher groupObjectMatcher;

    public ArrayList<Vertex> vertices = new ArrayList<Vertex>();
    public ArrayList<Vertex> vertexNormals = new ArrayList<Vertex>();
    public ArrayList<TextureCoordinate> textureCoordinates = new ArrayList<TextureCoordinate>();
    public ArrayList<HbmGroupObject> groupObjects = new ArrayList<HbmGroupObject>();
    private HbmGroupObject currentGroupObject;
    private String fileName;

    public HbmModelObject(ResourceLocation resource) throws ModelFormatException
    {
        this.fileName = resource.toString();

        try
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            loadObjModel(res.getInputStream());
        }
        catch (IOException e)
        {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    public HbmModelObject(String filename, InputStream inputStream) throws ModelFormatException
    {
        this.fileName = filename;
        loadObjModel(inputStream);
    }

    private void loadObjModel(InputStream inputStream) throws ModelFormatException
    {
        BufferedReader reader = null;

        String currentLine = null;
        int lineCount = 0;

        try
        {
            reader = new BufferedReader(new InputStreamReader(inputStream));

            while ((currentLine = reader.readLine()) != null)
            {
                lineCount++;
                currentLine = currentLine.replaceAll("\\s+", " ").trim();

                if (currentLine.startsWith("#") || currentLine.length() == 0)
                {
                    continue;
                }
                else if (currentLine.startsWith("v "))
                {
                    Vertex vertex = parseVertex(currentLine, lineCount);
                    if (vertex != null)
                    {
                        vertices.add(vertex);
                    }
                }
                else if (currentLine.startsWith("vn "))
                {
                    Vertex vertex = parseVertexNormal(currentLine, lineCount);
                    if (vertex != null)
                    {
                        vertexNormals.add(vertex);
                    }
                }
                else if (currentLine.startsWith("vt "))
                {
                    TextureCoordinate textureCoordinate = parseTextureCoordinate(currentLine, lineCount);
                    if (textureCoordinate != null)
                    {
                        textureCoordinates.add(textureCoordinate);
                    }
                }
                else if (currentLine.startsWith("f "))
                {

                    if (currentGroupObject == null)
                    {
                        currentGroupObject = new HbmGroupObject("Default");
                    }

                    HbmFace face = parseFace(currentLine, lineCount);

                    if (face != null)
                    {
                        currentGroupObject.faces.add(face);
                    }
                }
                else if (currentLine.startsWith("g ") | currentLine.startsWith("o "))
                {
                	HbmGroupObject group = parseGroupObject(currentLine, lineCount);

                    if (group != null)
                    {
                        if (currentGroupObject != null)
                        {
                            groupObjects.add(currentGroupObject);
                        }
                    }

                    currentGroupObject = group;
                }
            }

            groupObjects.add(currentGroupObject);
        }
        catch (IOException e)
        {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                // hush
            }

            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                // hush
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderAll()
    {
        Tessellator tessellator = Tessellator.instance;

        if (currentGroupObject != null)
        {
            tessellator.startDrawing(currentGroupObject.glDrawingMode);
        }
        else
        {
            tessellator.startDrawing(GL11.GL_TRIANGLES);
        }
        tessellateAll(tessellator);

        tessellator.draw();
    }

    @SideOnly(Side.CLIENT)
    public void tessellateAll(Tessellator tessellator)
    {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            groupObject.render(currentTime, tessellator);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderOnly(String... groupNames)
    {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            for (String groupName : groupNames)
            {
                if (groupName.equalsIgnoreCase(groupObject.name))
                {
                    groupObject.render(currentTime);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tessellateOnly(Tessellator tessellator, String... groupNames) {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            for (String groupName : groupNames)
            {
                if (groupName.equalsIgnoreCase(groupObject.name))
                {
                    groupObject.render(currentTime, tessellator);
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderPart(String partName)
    {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            if (partName.equalsIgnoreCase(groupObject.name))
            {
                groupObject.render(currentTime);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tessellatePart(Tessellator tessellator, String partName) {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            if (partName.equalsIgnoreCase(groupObject.name))
            {
                groupObject.render(currentTime, tessellator);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderAllExcept(String... excludedGroupNames)
    {
	float currentTime = Clock.get_ms();
        for (HbmGroupObject groupObject : groupObjects)
        {
            boolean skipPart=false;
            for (String excludedGroupName : excludedGroupNames)
            {
                if (excludedGroupName.equalsIgnoreCase(groupObject.name))
                {
                    skipPart=true;
                }
            }
            if(!skipPart)
            {
                groupObject.render(currentTime);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void tessellateAllExcept(Tessellator tessellator, String... excludedGroupNames)
    {
	float currentTime = Clock.get_ms();
        boolean exclude;
        for (HbmGroupObject groupObject : groupObjects)
        {
            exclude=false;
            for (String excludedGroupName : excludedGroupNames)
            {
                if (excludedGroupName.equalsIgnoreCase(groupObject.name))
                {
                    exclude=true;
                }
            }
            if(!exclude)
            {
                groupObject.render(currentTime, tessellator);
            }
        }
    }

    private Vertex parseVertex(String line, int lineCount) throws ModelFormatException
    {
        Vertex vertex = null;

        if (isValidVertexLine(line))
        {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try
            {
                if (tokens.length == 2)
                {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
                }
                else if (tokens.length == 3)
                {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
                }
            }
            catch (NumberFormatException e)
            {
                throw new ModelFormatException(String.format("Number formatting error at line %d",lineCount), e);
            }
        }
        else
        {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return vertex;
    }

    private Vertex parseVertexNormal(String line, int lineCount) throws ModelFormatException
    {
        Vertex vertexNormal = null;

        if (isValidVertexNormalLine(line))
        {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try
            {
                if (tokens.length == 3)
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            }
            catch (NumberFormatException e)
            {
                throw new ModelFormatException(String.format("Number formatting error at line %d",lineCount), e);
            }
        }
        else
        {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return vertexNormal;
    }

    private TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws ModelFormatException
    {
        TextureCoordinate textureCoordinate = null;

        if (isValidTextureCoordinateLine(line))
        {
            line = line.substring(line.indexOf(" ") + 1);
            String[] tokens = line.split(" ");

            try
            {
                if (tokens.length == 2)
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]));
                else if (tokens.length == 3)
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            }
            catch (NumberFormatException e)
            {
                throw new ModelFormatException(String.format("Number formatting error at line %d",lineCount), e);
            }
        }
        else
        {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return textureCoordinate;
    }

    private HbmFace parseFace(String line, int lineCount) throws ModelFormatException
    {
    	HbmFace face = null;

        if (isValidFaceLine(line))
        {
            face = new HbmFace();

            String trimmedLine = line.substring(line.indexOf(" ") + 1);
            String[] tokens = trimmedLine.split(" ");
            String[] subTokens = null;

            if (tokens.length == 3)
            {
                if (currentGroupObject.glDrawingMode == -1)
                {
                    currentGroupObject.glDrawingMode = GL11.GL_TRIANGLES;
                }
                else if (currentGroupObject.glDrawingMode != GL11.GL_TRIANGLES)
                {
                    throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 4, found " + tokens.length + ")");
                }
            }
            else if (tokens.length == 4)
            {
                if (currentGroupObject.glDrawingMode == -1)
                {
                    currentGroupObject.glDrawingMode = GL11.GL_QUADS;
                }
                else if (currentGroupObject.glDrawingMode != GL11.GL_QUADS)
                {
                    throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 3, found " + tokens.length + ")");
                }
            }

            // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
            if (isValidFace_V_VT_VN_Line(line))
            {
                face.vertices = new Vertex[tokens.length];
                face.textureCoordinates = new TextureCoordinate[tokens.length];
                face.vertexNormals = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; ++i)
                {
                    subTokens = tokens[i].split("/");

                    face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    face.vertexNormals[i] = vertexNormals.get(Integer.parseInt(subTokens[2]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            }
            // f v1/vt1 v2/vt2 v3/vt3 ...
            else if (isValidFace_V_VT_Line(line))
            {
                face.vertices = new Vertex[tokens.length];
                face.textureCoordinates = new TextureCoordinate[tokens.length];

                for (int i = 0; i < tokens.length; ++i)
                {
                    subTokens = tokens[i].split("/");

                    face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.textureCoordinates[i] = textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            }
            // f v1//vn1 v2//vn2 v3//vn3 ...
            else if (isValidFace_V_VN_Line(line))
            {
                face.vertices = new Vertex[tokens.length];
                face.vertexNormals = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; ++i)
                {
                    subTokens = tokens[i].split("//");

                    face.vertices[i] = vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    face.vertexNormals[i] = vertexNormals.get(Integer.parseInt(subTokens[1]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            }
            // f v1 v2 v3 ...
            else if (isValidFace_V_Line(line))
            {
                face.vertices = new Vertex[tokens.length];

                for (int i = 0; i < tokens.length; ++i)
                {
                    face.vertices[i] = vertices.get(Integer.parseInt(tokens[i]) - 1);
                }

                face.faceNormal = face.calculateFaceNormal();
            }
            else
            {
                throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
            }
        }
        else
        {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return face;
    }

    private HbmGroupObject parseGroupObject(String line, int lineCount) throws ModelFormatException
    {
    	HbmGroupObject group = null;

        if (isValidGroupObjectLine(line))
        {
            String trimmedLine = line.substring(line.indexOf(" ") + 1);

            if (trimmedLine.length() > 0)
            {
                group = new HbmGroupObject(trimmedLine);
            }
        }
        else
        {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return group;
    }

    /***
     * Verifies that the given line from the model file is a valid vertex
     * @param line the line being validated
     * @return true if the line is a valid vertex, false otherwise
     */
    private static boolean isValidVertexLine(String line)
    {
        if (vertexMatcher != null)
        {
            vertexMatcher.reset();
        }

        vertexMatcher = vertexPattern.matcher(line);
        return vertexMatcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid vertex normal
     * @param line the line being validated
     * @return true if the line is a valid vertex normal, false otherwise
     */
    private static boolean isValidVertexNormalLine(String line)
    {
        if (vertexNormalMatcher != null)
        {
            vertexNormalMatcher.reset();
        }

        vertexNormalMatcher = vertexNormalPattern.matcher(line);
        return vertexNormalMatcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid texture coordinate
     * @param line the line being validated
     * @return true if the line is a valid texture coordinate, false otherwise
     */
    private static boolean isValidTextureCoordinateLine(String line)
    {
        if (textureCoordinateMatcher != null)
        {
            textureCoordinateMatcher.reset();
        }

        textureCoordinateMatcher = textureCoordinatePattern.matcher(line);
        return textureCoordinateMatcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices, texture coordinates, and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1/vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VT_VN_Line(String line)
    {
        if (face_V_VT_VN_Matcher != null)
        {
            face_V_VT_VN_Matcher.reset();
        }

        face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line);
        return face_V_VT_VN_Matcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and texture coordinates
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VT_Line(String line)
    {
        if (face_V_VT_Matcher != null)
        {
            face_V_VT_Matcher.reset();
        }

        face_V_VT_Matcher = face_V_VT_Pattern.matcher(line);
        return face_V_VT_Matcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1//vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VN_Line(String line)
    {
        if (face_V_VN_Matcher != null)
        {
            face_V_VN_Matcher.reset();
        }

        face_V_VN_Matcher = face_V_VN_Pattern.matcher(line);
        return face_V_VN_Matcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by only vertices
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_Line(String line)
    {
        if (face_V_Matcher != null)
        {
            face_V_Matcher.reset();
        }

        face_V_Matcher = face_V_Pattern.matcher(line);
        return face_V_Matcher.matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face of any of the possible face formats
     * @param line the line being validated
     * @return true if the line is a valid face that matches any of the valid face formats, false otherwise
     */
    private static boolean isValidFaceLine(String line)
    {
        return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(line);
    }

    /***
     * Verifies that the given line from the model file is a valid group (or object)
     * @param line the line being validated
     * @return true if the line is a valid group (or object), false otherwise
     */
    private static boolean isValidGroupObjectLine(String line)
    {
        if (groupObjectMatcher != null)
        {
            groupObjectMatcher.reset();
        }

        groupObjectMatcher = groupObjectPattern.matcher(line);
        return groupObjectMatcher.matches();
    }

    @Override
    public String getType()
    {
        return "hmf";
    }
}
