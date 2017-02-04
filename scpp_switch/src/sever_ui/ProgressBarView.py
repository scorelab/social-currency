from Tkinter import *


class ProgressBarView:
    def __init__(self, master=None, orientation='horizontal',
                 min=0, max=100, width=100, height=None,
                 doLabel=1, appearance=None,
                 fillColor=None, background=None,
                 labelColor=None, labelFont=None,
                 labelText='', labelFormat="%d%%",
                 value=0.1, bd=2):
        # preserve various values
        self.master = master
        self.orientation = orientation
        self.min = min
        self.max = max
        self.doLabel = doLabel
        self.labelText = labelText
        self.labelFormat = labelFormat
        self.value = value
        if (fillColor == None) or (background == None) or (labelColor == None):
            # We have no system color names under linux. So use a workaround.
            # btn = Button(font=labelFont)
            btn = Button(master, text='0', font=labelFont)
            if fillColor == None:
                fillColor = btn['foreground']
            if background == None:
                background = btn['disabledforeground']
            if labelColor == None:
                labelColor = btn['background']
        if height == None:
            l = Label(font=labelFont)
            height = l.winfo_reqheight()
        self.width = width
        self.height = height
        self.fillColor = fillColor
        self.labelFont = labelFont
        self.labelColor = labelColor
        self.background = background
        #
        # Create components
        #
        self.frame = Frame(master, relief=appearance, bd=bd, width=width, height=height)
        self.canvas = Canvas(self.frame, bd=0,
                             highlightthickness=0, background=background, width=width, height=height)
        self.scale = self.canvas.create_rectangle(0, 0, width, height,
                                                  fill=fillColor)
        self.label = self.canvas.create_text(width / 2, height / 2,
                                             text=labelText, anchor=CENTER, fill=labelColor, font=self.labelFont)
        self.canvas.pack(fill=BOTH)
        self.update()
        self.canvas.bind('<Configure>', self.onResize)  # monitor size changes

    def onResize(self, event):
        if (self.width == event.width) and (self.height == event.height):
            return
        # Set new sizes
        self.width = event.width
        self.height = event.height
        # Move label
        self.canvas.coords(self.label, event.width / 2, event.height / 2)
        # Display bar in new sizes
        self.update()

    def updateProgress(self, newValue, newMax=None):
        if newMax:
            self.max = newMax
        self.value = newValue
        self.update()

    def pack(self, *args, **kw):
        self.frame.pack(*args, **kw)

    def update(self):
        # Trim the values to be between min and max
        value = self.value
        if value > self.max:
            value = self.max
        if value < self.min:
            value = self.min
        # Adjust the rectangle
        if self.orientation == "horizontal":
            self.canvas.coords(self.scale, 0, 0,
                               float(value) / self.max * self.width, self.height)
        else:
            self.canvas.coords(self.scale, 0,
                               self.height - (float(value) / self.max * self.height),
                               self.width, self.height)
        # And update the label
        if self.doLabel:
            if value:
                if value >= 0:
                    pvalue = int((float(value) / float(self.max)) * 100.0)
                else:
                    pvalue = 0
                self.canvas.itemconfig(self.label, text=self.labelFormat % pvalue)
            else:
                self.canvas.itemconfig(self.label, text='')
        else:
            self.canvas.itemconfig(self.label, text=self.labelFormat %
                                                    self.labelText)
        self.canvas.update_idletasks()
