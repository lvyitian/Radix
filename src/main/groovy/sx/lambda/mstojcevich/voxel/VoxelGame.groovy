package sx.lambda.mstojcevich.voxel

import groovy.transform.CompileStatic
import io.netty.channel.ChannelHandlerContext
import org.lwjgl.BufferUtils
import org.lwjgl.LWJGLException
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.util.glu.GLU
import sx.lambda.mstojcevich.voxel.api.VoxelGameAPI
import sx.lambda.mstojcevich.voxel.api.events.EventGameTick
import sx.lambda.mstojcevich.voxel.api.events.EventWorldStart
import sx.lambda.mstojcevich.voxel.client.gui.GuiScreen
import sx.lambda.mstojcevich.voxel.client.gui.screens.IngameHUD
import sx.lambda.mstojcevich.voxel.entity.EntityRotation
import sx.lambda.mstojcevich.voxel.net.packet.client.PacketLeaving
import sx.lambda.mstojcevich.voxel.render.Renderer
import sx.lambda.mstojcevich.voxel.settings.SettingsManager
import sx.lambda.mstojcevich.voxel.tasks.*
import sx.lambda.mstojcevich.voxel.tasks.InputHandler
import sx.lambda.mstojcevich.voxel.tasks.MovementHandler
import sx.lambda.mstojcevich.voxel.tasks.RepeatedTask
import sx.lambda.mstojcevich.voxel.util.Frustum
import sx.lambda.mstojcevich.voxel.world.IWorld
import sx.lambda.mstojcevich.voxel.world.World
import sx.lambda.mstojcevich.voxel.world.chunk.IChunk
import sx.lambda.mstojcevich.voxel.client.net.ClientConnection
import sx.lambda.mstojcevich.voxel.entity.EntityPosition
import sx.lambda.mstojcevich.voxel.entity.player.Player
import sx.lambda.mstojcevich.voxel.render.game.GameRenderer
import sx.lambda.mstojcevich.voxel.tasks.RotationHandler
import sx.lambda.mstojcevich.voxel.tasks.WorldLoader
import sx.lambda.mstojcevich.voxel.texture.TextureManager
import sx.lambda.mstojcevich.voxel.util.PlotCell3f
import sx.lambda.mstojcevich.voxel.util.Vec3i
import sx.lambda.mstojcevich.voxel.util.gl.ShaderManager
import sx.lambda.mstojcevich.voxel.util.gl.ShaderProgram

import javax.vecmath.Vector3f
import java.nio.FloatBuffer
import java.util.concurrent.LinkedBlockingDeque

import static org.lwjgl.opengl.GL11.*

@CompileStatic
public class VoxelGame {

    public static final boolean DEBUG = false

    private static VoxelGame theGame

    private SettingsManager settingsManager

    public static final String GAME_TITLE = "VoxelTest"

    private static final int START_WIDTH = 320, START_HEIGHT = 240; //TODO Config - screen size

    private int renderedFrames = 0;

    private IWorld world

    private Player player

    private boolean done

    private synchronized Vec3i selectedBlock, selectedNextPlace

    private int hudDisplayList

    private Queue<Runnable> glQueue = new LinkedBlockingDeque<>()

    private GuiScreen currentScreen

    private TextureManager textureManager = new TextureManager()
    private ShaderManager shaderManager = new ShaderManager()

    private Frustum frustum = new Frustum()

    private boolean calcFrustum

    private FloatBuffer lightPosition

    private Renderer renderer

    private ShaderProgram defaultShader

    private final boolean remote;
    private String hostname
    private short port
    private ChannelHandlerContext serverChanCtx;

    private RepeatedTask[] handlers = [
        new WorldLoader(this),
        new InputHandler(this),
        new MovementHandler(this),
        new RotationHandler(this)
    ]

    public static void main(String[] args) throws Exception {
        theGame = new VoxelGame("127.0.0.1", (short)31173);
        theGame.start();
    }

    VoxelGame() {
        remote = false
    }

    VoxelGame(String hostname, short port) {
        remote = true
        this.hostname = hostname
        this.port = port
    }

    private void start() throws LWJGLException {
        settingsManager = new SettingsManager()
        setupWindow()
        setupOGL()
        world = new World(remote, false)
        player = new Player(new EntityPosition(0, 256, 0), new EntityRotation(0, 0))
        player.init()
        world.addEntity player
        currentScreen = new IngameHUD()
        currentScreen.init()
        startHandlers()
        setRenderer(new GameRenderer(this))
        hudDisplayList = glGenLists 1
        rerenderHud()
        if(remote) {
            new Thread() {
                @Override
                public void run() {
                    new ClientConnection(hostname, port).start()
                }
            }.start()
        }
        if(!remote) {
            world.loadChunks(new EntityPosition(0, 0, 0), settingsManager.visualSettings.viewDistance)
        }

        new Thread() {
            @Override
            public void run() {
                while(!done) {
                    player.onUpdate()

                    VoxelGameAPI.instance.eventManager.push(new EventGameTick(world))

                    sleep(50l);
                }
            }
        }.start()

        VoxelGameAPI.instance.eventManager.push(new EventWorldStart())
        run()
    }

    private void startHandlers() {
        for(RepeatedTask r : handlers) {
            new Thread(r, r.identifier).start()
        }
    }

    private void setupWindow() throws LWJGLException {
        Display.setFullscreen settingsManager.visualSettings.fullscreen

        Display.setTitle GAME_TITLE

        Display.setDisplayMode(new DisplayMode(START_WIDTH, START_HEIGHT))

        Display.setResizable true

        Display.create()

        Mouse.setGrabbed true
    }

    private void setupOGL() {
        glEnable GL_TEXTURE_2D
        glShadeModel GL_SMOOTH
        glClearColor(0.2f, 0.4f, 1, 0) //Set default color
        glClearDepth 1 //Set default depth buffer depth
        glEnable GL_DEPTH_TEST //Enable depth visibility check
        glDepthFunc GL_LEQUAL //How to test depth (less than or equal)

        glEnableClientState GL_VERTEX_ARRAY
        glEnableClientState GL_COLOR_ARRAY

        glMatrixMode GL_PROJECTION //Currently altering projection matrix
        glLoadIdentity()

        frustum.calculateFrustum()

        glMatrixMode GL_MODELVIEW //Currently altering modelview matrix
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST)
        glEnable GL_CULL_FACE
        glFrontFace GL_CCW

        lightPosition = BufferUtils.createFloatBuffer(4)
        lightPosition.put(0).put(256).put(0).put(1).flip()

        glEnable GL_LIGHTING
        glEnable GL_LIGHT0

        FloatBuffer diffuseVec = BufferUtils.createFloatBuffer(4)
        diffuseVec.put(2, 2, 2, 1.0f)
        diffuseVec.flip()

        glLightModel(GL_LIGHT_MODEL_AMBIENT, diffuseVec)

//        defaultShader = createShader("default")
//
//        getShaderManager().setShader(defaultShader)
    }

    private void run() {
        long startTime = System.currentTimeMillis()
        while (!Display.isCloseRequested() && !done) {
            render()

            Display.update()
            Display.sync settingsManager.visualSettings.maxFPS

            if (renderedFrames % 100 == 0) {
                println renderedFrames / ((System.currentTimeMillis() - startTime) / 1000)
            }
        }
        done = true
        Display.destroy()
        if(isRemote() && serverChanCtx != null) {
            serverChanCtx.writeAndFlush(new PacketLeaving("Game closed"))
            serverChanCtx.disconnect()
        }
    }

    private void render() {
        prepareNewFrame()

        runQueuedOGL()

        if(renderer != null) {
            glPushMatrix()

            renderer.render()

            glPopMatrix()
        }

        //2D starts here
        glPushMatrix()
        glPushAttrib GL_ENABLE_BIT
        prepare2D()

        glCallList hudDisplayList //TODO move to HUD GUI
        currentScreen.render true //Render as ingame

        glPopMatrix()
        glPopAttrib()

        renderedFrames++
        Thread.yield()
    }

    private void runQueuedOGL() {
        Runnable currentRunnable
        while ((currentRunnable = glQueue.poll()) != null) {
            currentRunnable.run()
        }
    }

    private static void prepare2D() {
        glMatrixMode GL_PROJECTION
        glLoadIdentity()
        glOrtho(0, Display.width, Display.height, 0, -1, 1)
        glMatrixMode GL_MODELVIEW
        glLoadIdentity()
        glColor4f(1, 1, 1, 1)
        glDisable GL_LIGHTING
        glDisable GL_DEPTH_TEST
    }

    private void prepareNewFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        glMatrixMode GL_PROJECTION //Currently altering projection matrix
        glLoadIdentity()

        GLU.gluPerspective(100, (float) Display.width / Display.height, 0.1f,
                settingsManager.visualSettings.viewDistance * world.chunkSize);
        //Set up camera

        glMatrixMode GL_MODELVIEW //Currently altering modelview matrix
        glLoadIdentity()

        textureManager.bindTexture(-1)
    }

    public void updateSelectedBlock() {
        PlotCell3f plotter = new PlotCell3f(0, 0, 0, 1, 1, 1)
        float x = player.position.x
        float y = player.position.y
        float z = player.position.z
        float pitch = player.rotation.pitch
        float yaw = player.rotation.yaw
        float reach = player.reach

        float deltaX = (float)(Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)))
        float deltaY = (float)(Math.sin(Math.toRadians(pitch)))
        float deltaZ = (float)(-Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw)))

        plotter.plot(new Vector3f(x, y, z), new Vector3f(deltaX, deltaY, deltaZ), (int) Math.ceil(reach * reach))
        Vec3i last
        while (plotter.next()) {
            Vec3i v = plotter.get()
            Vec3i bp = new Vec3i(v.x, v.y, v.z)
            IChunk theChunk = world.getChunkAtPosition(bp)
            if (theChunk != null) {
                if (theChunk.getBlockAtPosition(bp) != null) {
                    selectedBlock = bp
                    if (last != null) {
                        if (theChunk.getBlockAtPosition(last) == null) {
                            selectedNextPlace = last
                        }
                    }
                    plotter.end()
                    return
                }
                last = bp;
            }
        }
        selectedNextPlace = null
        selectedBlock = null
    }

    public void addToGLQueue(Runnable runnable) {
        glQueue.add runnable
    }

    public static VoxelGame getInstance() {
        theGame
    }

    public void rerenderHud() {
        if(player == null || world == null)return
        glNewList(hudDisplayList, GL_COMPILE)
        player.itemInHand.renderer.render2d(0, 0, 20)
        glEndList()
    }

    public IWorld getWorld() { world }

    public Player getPlayer() { player }

    public TextureManager getTextureManager() { textureManager }

    public ShaderManager getShaderManager() { shaderManager }

    public Frustum getFrustum() { frustum }

    public boolean isDone() { done }

    public SettingsManager getSettingsManager() { settingsManager }

    public Vec3i getSelectedBlock() { selectedBlock }

    public Vec3i getNextPlacePos() { selectedNextPlace }

    public void startShutdown() {
        done = true
    }

    public void calculateFrustum() {
        this.calcFrustum = true
    }

    public boolean shouldCalcFrustum() { calcFrustum }

    public void dontCalcFrustum() { calcFrustum = false } //TODO remove this when frustum stuff is in GameRenderer

    public FloatBuffer getLightPosition() { lightPosition }

    private void setRenderer(Renderer renderer) {
        this.renderer = renderer
        this.renderer.init()
    }

    private ShaderProgram createShader(String shaderName) {
        String nameNoExt = "/shaders/$shaderName/$shaderName"
        String vertex = this.class.getResourceAsStream(nameNoExt + ".vert").text
        String fragment = this.class.getResourceAsStream(nameNoExt + ".frag").text

        ShaderProgram program = new ShaderProgram(vertex, fragment);

        if(!program.log.isEmpty()) {
            System.err.println program.log
            return null
        }

        return program
    }

    public boolean isRemote() {
        return remote
    }

    public ChannelHandlerContext getServerChanCtx() { serverChanCtx }

    public void setServerChanCtx(ChannelHandlerContext ctx) { serverChanCtx = ctx }

    //TODO move frustum calc, light pos, etc into GameRenderer

}
